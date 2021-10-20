package com.imf.so.assets.load.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: lixiaoliang
 * @Date: 2021/10/20 2:46 PM
 * @Description:
 */
public final class LoadUtils {
    private static String sProcessName = "";

    private LoadUtils() {
    }

    public static String getCurrentProcessNameByContext(Context context) {
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

    public static String getCurrentProcessByCache() {
        return sProcessName;
    }

    public static String[] supportedAbis() {
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS;
        } else if (!TextUtils.isEmpty(Build.CPU_ABI2)) {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        } else {
            return new String[]{Build.CPU_ABI};
        }
    }


    public String mapLibraryName(final String libraryName) {
        if (libraryName.startsWith("lib") && libraryName.endsWith(".so")) {
            return libraryName;
        }
        return System.mapLibraryName(libraryName);
    }

    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    public static Boolean object2Boolean(Object value) {
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


    public static String loadStringByInputStream(InputStream open) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder stringBuilder = null;
        try {
            inputStreamReader = new InputStreamReader(open, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            //ignore
        } finally {
            LoadUtils.closeStreams(bufferedReader, inputStreamReader, open);
        }
        return null;
    }

    public static void closeStreams(Closeable... closeables) {
        if (closeables != null && closeables.length > 0) {
            for (int i = 0; i < closeables.length; i++) {
                Closeable closeable = closeables[i];
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
    }

}