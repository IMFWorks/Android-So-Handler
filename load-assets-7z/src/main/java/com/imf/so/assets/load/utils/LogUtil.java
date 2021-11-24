package com.imf.so.assets.load.utils;

import android.util.Log;

/**
 * @Author: lixiaoliang
 * @Date: 2021/10/21 4:43 PM
 * @Description:
 */
public final class LogUtil {
    private static final String TAG_DEFAULT = "jniLibs";
    private static boolean sLogEnable = true;

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private LogUtil() {
    }

    public static void setLogEnable(boolean logEnable) {
        sLogEnable = logEnable;
    }

    private static void print(int priority, String tag, String msg) {
        Log.println(priority, tag == null ? TAG_DEFAULT : tag, msg);
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
        if (sLogEnable) {
            StringBuilder builder = joinBuilder("Thread(", Thread.currentThread().getName(), ")");
            for (Object s : msg) {
                builder.append(s);
            }
            print(DEBUG, null, builder.toString());
        }
    }

    public static void printError(boolean force, Object... msg) {
        if (sLogEnable || force) {
            if (msg != null && msg.length > 0) {
                StringBuilder builder = new StringBuilder();
                for (Object s : msg) {
                    builder.append(s);
                }
                print(ERROR, null, builder.toString());
            }
        }
    }

    public static void printThrowable(Throwable error, Object... msg) {
        printError(true, msg);
        error.printStackTrace();
    }

}