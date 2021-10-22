package com.imf.test;

public class NativeSourceTest {

    static {

    }

    public static native String stringFromJNI();

    public static void load() {
        System.loadLibrary("source");
    }
}