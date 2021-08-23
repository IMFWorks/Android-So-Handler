package com.imf.so.assets.load;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;


import com.imf.so.KeepSystemLoadLib;
import com.imf.so.SoLoadHook;
import com.hzy.lib7z.Z7Extractor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/19 2:59 PM
 * @Description:
 */
@KeepSystemLoadLib
public class AssetsSoLoadBy7zFileManager {
    public final static String DIR_JNI_LIBS = "jniLibs";
    private final static String[] SUPPORTED_ABIS = supportedAbis();
    private static Context appContext;
    private static File saveLibsDir;
    private static JSONObject[] supportedAbisInfo = new JSONObject[SUPPORTED_ABIS.length];
    private static String sProcessName;
    static final Set<String> sLoadedLibraries = Collections.synchronizedSet(new HashSet<String>());

    public static String getCurrentProcessName(Context context) {
        if (TextUtils.isEmpty(sProcessName)) {
            int pid = android.os.Process.myPid();
            ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    sProcessName = appProcess.processName;
                    break;
                }
            }
        }
        return sProcessName;
    }

    private static String[] supportedAbis() {
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS;
        } else if (!TextUtils.isEmpty(Build.CPU_ABI2)) {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        } else {
            return new String[]{Build.CPU_ABI};
        }
    }

    public static void init(Context context) {
        if (loadSoInfoFile(context)) {
            appContext = context.getApplicationContext();
            saveLibsDir = context.getDir(DIR_JNI_LIBS, Context.MODE_PRIVATE);
        }
        getCurrentProcessName(context);
        SoLoadHook.setSoLoadProxy(new AssetsSoLoadBy7z());
    }

    private static boolean loadSoInfoFile(Context context) {
        InputStream open = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = null;
        try {
            open = context.getAssets().open(DIR_JNI_LIBS + "/info.json");
            inputStreamReader = new InputStreamReader(open, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            for (int i = 0; i < SUPPORTED_ABIS.length; i++) {
                String supportedAbi = SUPPORTED_ABIS[i];
                JSONObject abiInfo = jsonObject.optJSONObject(supportedAbi);
                if (abiInfo != null) {
                    supportedAbisInfo[i] = abiInfo;
                }
            }
            return true;
        } catch (Exception e) {
            //ignore
        } finally {
            closeStream(bufferedReader);
            closeStream(inputStreamReader);
            closeStream(open);
        }
        return false;
    }

    private static void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    public static SoFileInfo getLibSoInfo(String libName) {
        if (supportedAbisInfo == null || supportedAbisInfo.length == 0) {
            return null;
        }
        for (int i = 0; i < supportedAbisInfo.length; i++) {
            JSONObject jsonObject = supportedAbisInfo[i];
            if (jsonObject != null) {
                JSONObject targetSoInfo = jsonObject.optJSONObject(libName);
                if (targetSoInfo == null) {
                    continue;
                }
                String md5 = targetSoInfo.optString("md5", null);
                if (TextUtils.isEmpty(md5)) {
                    continue;
                }
                Object object = targetSoInfo.opt("saveCompressToAssets");
                if (object == null) {
                    continue;
                }
                boolean saveCompressToAssets = object2Boolean(object);
                String compressName = targetSoInfo.optString("compressName", null);
                JSONArray dependenciesJSONArray = targetSoInfo.optJSONArray("dependencies");
                List<String> dependencies = null;
                if (dependenciesJSONArray != null && dependenciesJSONArray.length() > 0) {
                    dependencies = new ArrayList<>(dependenciesJSONArray.length());
                    for (int j = 0; j < dependenciesJSONArray.length(); j++) {
                        String item = dependenciesJSONArray.optString(j);
                        if (!TextUtils.isEmpty(item)) {
                            dependencies.add(item);
                        }
                    }
                }
                return new SoFileInfo(SUPPORTED_ABIS[i], saveCompressToAssets, md5, compressName, dependencies);
            }
        }
        return null;
    }

    private static Boolean object2Boolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String stringValue = (String) value;
            if ("true".equalsIgnoreCase(stringValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return false;
            }
        }
        return null;
    }

    public String mapLibraryName(final String libraryName) {
        if (libraryName.startsWith("lib") && libraryName.endsWith(".so")) {
            return libraryName;
        }
        return System.mapLibraryName(libraryName);
    }

    public static String joinPath(final String... paths) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            stringBuilder.append(File.separatorChar).append(paths[i]);
        }
        return stringBuilder.toString();
    }

    public static String joinString(String separator, String... strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            stringBuilder.append(separator).append(strings[i]);
        }
        return stringBuilder.toString();
    }

    public static void loadLibraryAndDependencies(String libName) {
        if (sLoadedLibraries.contains(libName)) {
            Log.d(DIR_JNI_LIBS, "重复加载" + libName);
            return;
        }
        Log.d(DIR_JNI_LIBS, joinString(" - ", sProcessName, libName, Boolean.toString(Looper.getMainLooper() == Looper.myLooper())));
        SoFileInfo libSoInfo = AssetsSoLoadBy7zFileManager.getLibSoInfo(libName);
        if (libSoInfo != null) {
            loadItem(libName, libSoInfo);
        } else {
            sLoadedLibraries.add(libName);
            System.loadLibrary(libName);
        }
    }

    private static void loadItem(String libName, SoFileInfo libSoInfo) {
        if (libSoInfo.dependencies != null && !libSoInfo.dependencies.isEmpty()) {
            for (String dependency : libSoInfo.dependencies) {
                loadLibraryAndDependencies(dependency);
            }
        }
        if (libSoInfo.saveCompressToAssets && !TextUtils.isEmpty(libSoInfo.compressName)) {
            File source = new File(saveLibsDir, joinPath(libSoInfo.abi, libName, libSoInfo.md5));
            if (source.exists()) {
                sLoadedLibraries.add(libName);
                System.load(source.getAbsolutePath());
            } else {
                String assetsFile = joinPath(DIR_JNI_LIBS, libSoInfo.abi, libSoInfo.compressName);
                String outPath = source.getParent();
                Z7Extractor.extractAsset(appContext.getAssets(), assetsFile, outPath, new SingleFileExtractLoadSoFile(libName));
            }
        } else {
            sLoadedLibraries.add(libName);
            System.loadLibrary(libName);
        }
    }


    @KeepSystemLoadLib
    private static class SingleFileExtractLoadSoFile extends SingleFileExtractSoFile {
        String libName;

        public SingleFileExtractLoadSoFile(String libName) {
            this.libName = libName;
        }

        @Override
        public void onSingleFileExtractSucceed(File extractFile) {
            if (extractFile.exists()) {
                try {
                    // 设置权限为 rwxr-xr-extractFile
                    configPermisstion(extractFile);
                    sLoadedLibraries.add(libName);
                    System.load(extractFile.getAbsolutePath());
                } catch (Exception e) {
                    deleteFile(extractFile);
                    e.printStackTrace();
                }
            } else {
                Log.e(DIR_JNI_LIBS, libName + "解压成功后加载出错:" + extractFile.getAbsolutePath());
            }
        }


        private void configPermisstion(File soFile) {
            soFile.setReadable(true, false);
            soFile.setExecutable(true, false);
            soFile.setWritable(true);
        }

        private static void deleteFile(File file) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }
}