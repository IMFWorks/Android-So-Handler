package com.imf.so.assets.load;

import android.util.Log;

import com.hzy.lib7z.IExtractCallback;

import java.io.File;

abstract class SingleFileExtractSoFile implements IExtractCallback {
    private File extractFile;

    @Override
    public void onStart() {

    }

    @Override
    public void onGetFileNum(int i) {

    }

    @Override
    public void onProgress(String dirName, String name, long size) {
        extractFile = new File(dirName, name);
        Log.d(AssetsSoLoadBy7zFileManager.DIR_JNI_LIBS, "开始解压:" + AssetsSoLoadBy7zFileManager.joinPath(extractFile.getParent(), name));
    }

    @Override
    public void onError(int i, String s) {
        Log.d(AssetsSoLoadBy7zFileManager.DIR_JNI_LIBS, "解压失败:" + extractFile.getName() + " , erroInfo:" + s);
    }

    @Override
    public void onSucceed() {
        Log.d(AssetsSoLoadBy7zFileManager.DIR_JNI_LIBS, "解压完成:" + extractFile.getName());
        onSingleFileExtractSucceed(extractFile);
    }

    public abstract void onSingleFileExtractSucceed(File extractFile);
}