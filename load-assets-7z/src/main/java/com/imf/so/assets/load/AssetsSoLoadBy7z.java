package com.imf.so.assets.load;

import com.imf.so.SoLoadProxy;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/19 3:21 PM
 * @Description:
 */
public class AssetsSoLoadBy7z implements SoLoadProxy {

    @Override
    public void loadLibrary(String libName) {
        AssetsSoLoadBy7zFileManager.loadLibraryAndDependencies(libName);
    }

    @Override
    public void load(String fileName) {
        System.load(fileName);
    }
}