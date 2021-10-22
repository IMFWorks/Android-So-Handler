package com.imf.so.assets.load.extract;

import android.util.Log;

import com.imf.so.assets.load.utils.LoadRecordHelp;

import java.io.File;

public class SingleSoFileExtractAndLoad extends SingleSoFileExtract {
    private static final String TAG = "SoFileExtract";
    String libName;

    public SingleSoFileExtractAndLoad(String libName) {
        this.libName = libName;
    }

    @Override
    public void onSingleFileExtractSucceed(File extractFile) {
        if (extractFile.exists()) {
            try {
                // 设置权限为 rwxr-xr-extractFile
                configPermisstion(extractFile);
                LoadRecordHelp.loadSoFile(extractFile.getAbsolutePath(), libName);
            } catch (Exception e) {
                deleteFile(extractFile);
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, libName + "解压成功后加载出错:" + extractFile.getAbsolutePath());
        }
    }


    private void configPermisstion(File soFile) {
        soFile.setReadable(true, false);
        soFile.setExecutable(true, false);
        soFile.setWritable(true);
    }

    private static void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}