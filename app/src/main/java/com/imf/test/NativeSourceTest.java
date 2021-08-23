package com.imf.test;

public class NativeSourceTest {

    static {
        System.loadLibrary("source");
    }

    public static native String stringFromJNI();
}