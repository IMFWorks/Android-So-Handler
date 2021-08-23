package com.imf.so;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/13 4:00 PM
 * @Description:
 */
public class DefaultSoLoadProxy implements SoLoadProxy {
    @Override
    public void loadLibrary(String libName) {
        System.loadLibrary(libName);
    }

    @Override
    public void load(String filename) {
        System.load(filename);
    }
}