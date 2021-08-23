package com.imf.testLibrary;

public class NativeTestLibrary {

    static {
        System.loadLibrary("testLibrary");
    }

    public static native String stringFromJNI();
}