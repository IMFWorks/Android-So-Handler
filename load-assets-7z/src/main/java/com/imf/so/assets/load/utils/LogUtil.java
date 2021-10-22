package com.imf.so.assets.load.utils;

import android.util.Log;

/**
 * @Author: lixiaoliang
 * @Date: 2021/10/21 4:43 PM
 * @Description:
 */
public final class LogUtil {
    private static final String TAG_DEFAULT = "jniLibs";

    private LogUtil() {
    }

    private static void print(String tag, String msg) {
        Log.d(tag == null ? TAG_DEFAULT : tag, msg);
    }

    private static StringBuilder joinBuilder(Object... str) {
        StringBuilder stringBuilder = new StringBuilder();
        if (str != null && str.length > 0) {
            for (Object s : str) {
                stringBuilder.append(s);
            }
        }
        return stringBuilder;
    }

    public static void printDebug(Object... msg) {
        StringBuilder builder = joinBuilder("Thread(", Thread.currentThread().getName(), ")");
        for (Object s : msg) {
            builder.append(s);
        }
        print(null, builder.toString());
    }
}