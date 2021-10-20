package com.imf.so.assets.load;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;


import com.imf.so.SoLoadHook;
import com.hzy.lib7z.Z7Extractor;
import com.imf.so.assets.load.bean.SoFileInfo;
import com.imf.so.assets.load.extract.SingleSoFileExtractAndLoad;
import com.imf.so.assets.load.utils.LoadRecord;
import com.imf.so.assets.load.utils.LoadUtils;

import org.json.JSONObject;

import java.io.File;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/19 2:59 PM
 * @Description:
 */
public class AssetsSoLoadBy7zFileManager {
    public final static String DIR_JNI_LIBS = "jniLibs";
    public final static String ASSETS_CONFIG_INFO_PATH = "jniLibs/info.json";
    private static Context sAppContext;
    private static String sSupportedAbi;
    private static File sSaveLibsDir;
    private static JSONObject sSupportedAbisInfo;
    //    private static final ReentrantReadWriteLock sSoSourcesLock = new ReentrantReadWriteLock();


    public static boolean init(Context context) {
        if (sSaveLibsDir == null) {
            JSONObject jsonObject = loadAssetsConfigJson(context);
            if (jsonObject == null) {
                Log.e(DIR_JNI_LIBS, "读取配置信息错误,导致初始化失败");
                return false;
            }
            String[] supportedAbis = LoadUtils.supportedAbis();
            for (int i = 0; i < supportedAbis.length; i++) {
                String abi = supportedAbis[i];
                JSONObject abiInfo = jsonObject.optJSONObject(abi);
                if (abiInfo != null) {
                    //这里只需确定支持的即可 系统不允许加载不同架构so文件
                    sSupportedAbisInfo = abiInfo;
                    sSupportedAbi = abi;
                    break;
                }
            }
            if (sSupportedAbisInfo == null) {
                Log.e(DIR_JNI_LIBS, "so配置abi,不支持该平台");
                return false;
            }
            sAppContext = context.getApplicationContext();
            sSaveLibsDir = context.getDir(DIR_JNI_LIBS, Context.MODE_PRIVATE);
            //读取进程名称
            LoadUtils.getCurrentProcessNameByContext(context);
            //设置加载代理
            SoLoadHook.setSoLoadProxy(new AssetsSoLoadBy7z());
        }
        return true;
    }


    /**
     * 读取assets中配置信息详情
     *
     * @param context
     * @return 返回读取是否成功
     */
    private static JSONObject loadAssetsConfigJson(Context context) {
        try {
            String infoJson = LoadUtils.loadStringByInputStream(context.getAssets().open(ASSETS_CONFIG_INFO_PATH));
            return new JSONObject(infoJson);
        } catch (Exception e) {
        }
        return null;
    }


    /**
     * loadLibrary so库加载入口
     *
     * @param libName
     */
    public static void loadLibraryAndDependencies(String libName) {
        if (LoadRecord.isLoaded(libName)) {
            Log.d(DIR_JNI_LIBS, "重复加载" + libName);
            return;
        }
        Log.d(DIR_JNI_LIBS, LoadUtils.joinString(" ", "进程:", LoadUtils.getCurrentProcessByCache(), "so库名称:", libName, "是否是主线程:", Boolean.toString(LoadUtils.isMainThread())));
        SoFileInfo libSoInfo = SoFileInfo.fromJson(sSupportedAbisInfo, sSupportedAbi, libName);
        if (libSoInfo != null) {
            loadBySoFileInfo(libSoInfo);
        } else {
            LoadRecord.loadSoLibrary(libName);
        }
    }

    /**
     * 按配置加载
     *
     * @param libSoInfo
     */
    private static void loadBySoFileInfo(SoFileInfo libSoInfo) {
        if (libSoInfo.dependencies != null && !libSoInfo.dependencies.isEmpty()) {
            for (String dependency : libSoInfo.dependencies) {
                loadLibraryAndDependencies(dependency);
            }
        }
        if (libSoInfo.saveCompressToAssets && !TextUtils.isEmpty(libSoInfo.compressName)) {
            File source = new File(sSaveLibsDir, LoadUtils.joinPath(libSoInfo.abi, libSoInfo.libName, libSoInfo.md5));
            //已经解压过,存在so文件直接加载
            if (source.exists()) {
                LoadRecord.loadSoFile(source.getAbsolutePath(), libSoInfo.libName);
            } else {
                loadByExtractAsset(libSoInfo, source);
            }
        } else {
            LoadRecord.loadSoLibrary(libSoInfo.libName);
        }
    }

    /**
     * 解压至指定文件加载
     *
     * @param libSoInfo
     * @param source
     */
    private static void loadByExtractAsset(SoFileInfo libSoInfo, File source) {
        String assetsFile = LoadUtils.joinPath(DIR_JNI_LIBS, libSoInfo.abi, libSoInfo.compressName);
        String outPath = source.getParent();
        AssetManager assets = sAppContext.getAssets();
//        //输出解压日志
//        try {
//            InputStream open = assets.open(assetsFile);
//            int available = open.available();
//            Log.d(DIR_JNI_LIBS, "准备解压:" + assetsFile + " , 解压前大小: " + (available >> 10) + "kb");
//            open.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Z7Extractor.extractAsset(assets, assetsFile, outPath, new SingleSoFileExtractAndLoad(libSoInfo.libName));
    }

}