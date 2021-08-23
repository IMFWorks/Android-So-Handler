package com.imf.so;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/13 3:53 PM
 * @Description:
 */
public interface SoLoadProxy {
    void loadLibrary(String libName);

    void load(String filename);
}