package com.imf.so.assets.load.utils;

import android.util.Log;

import com.imf.so.KeepSystemLoadLib;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: lixiaoliang
 * @Date: 2021/10/20 4:11 PM
 * @Description: so加载记录
 */
@KeepSystemLoadLib
public final class LoadRecordHelp {
    private static final String TAG = "LoadRecord";
    private static final Set<String> sLoadedLibraries = new HashSet<String>();
    private static final ReentrantReadWriteLock sSoSourcesLock = new ReentrantReadWriteLock();

    public static boolean isLoaded(String libName) {
        sSoSourcesLock.readLock().lock();
        boolean contains = sLoadedLibraries.contains(libName);
        sSoSourcesLock.readLock().unlock();
        return contains;
    }

    private static void setLoaded(String libName) {
        sLoadedLibraries.add(libName);
    }

    public static boolean loadSoFile(String soAbsolutePath, String libName) {
        boolean result = true;
        if (!isLoaded(libName)) {
            sSoSourcesLock.writeLock().lock();
            try {
                System.load(soAbsolutePath);
                setLoaded(libName);
            } catch (Throwable e) {
                result = false;
                Log.e(TAG, "LoadRecordHelp - loadSo: ", e);
            } finally {
                sSoSourcesLock.writeLock().unlock();
            }
        }
        return result;
    }

    public static boolean loadSoLibrary(String libName) {
        boolean result = true;
        if (!isLoaded(libName)) {
            sSoSourcesLock.writeLock().lock();
            try {
                System.loadLibrary(libName);
                setLoaded(libName);
            } catch (Throwable e) {
                result = false;
                Log.e(TAG, "LoadRecordHelp - loadSoLibrary: ", e);
            } finally {
                sSoSourcesLock.writeLock().unlock();
            }
        }
        return result;
    }

}