package com.imf.so.assets.load;

import java.util.List;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/19 4:08 PM
 * @Description:
 */
class SoFileInfo {
    String abi;
    boolean saveCompressToAssets;
    String md5;
    String compressName;
    List<String> dependencies;

    public SoFileInfo(String abi, boolean saveCompressToAssets, String md5, String compressName, List<String> dependencies) {
        this.abi = abi;
        this.saveCompressToAssets = saveCompressToAssets;
        this.md5 = md5;
        this.compressName = compressName;
        this.dependencies = dependencies;
    }
}