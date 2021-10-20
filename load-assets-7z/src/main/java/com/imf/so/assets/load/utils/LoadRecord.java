package com.imf.so.assets.load.utils;

import android.util.Log;

import com.imf.so.KeepSystemLoadLib;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: lixiaoliang
 * @Date: 2021/10/20 4:11 PM
 * @Description: so加载记录
 */
@KeepSystemLoadLib
public final class LoadRecord {
    private static final String TAG = "LoadRecord";
    private static final Set<String> sLoadedLibraries = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public static boolean isLoaded(String libName) {
        return sLoadedLibraries.contains(libName);
    }

    public static void setLoaded(String libName) {
        sLoadedLibraries.add(libName);
    }

    public static void loadSoFile(String soAbsolutePath, String libName) {
        if (!isLoaded(libName)) {
            try {
                System.load(soAbsolutePath);
                setLoaded(libName);
            } catch (Throwable e) {
                Log.e(TAG, "loadSo: ", e);
            }
        }
    }

    public static void loadSoLibrary(String libName) {
        if (!isLoaded(libName)) {
            try {
                System.loadLibrary(libName);
                setLoaded(libName);
            } catch (Throwable e) {
                Log.e(TAG, "loadSoLibrary: ", e);
            }
        }
    }
}