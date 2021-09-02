package com.imf.so.assets.load;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.hzy.lib7z.IExtractCallback;

import java.io.File;

abstract class SingleFileExtractSoFile implements IExtractCallback {
    private File extractFile;
    private static long sumTime = 0l;
    private long time;

    @Override
    public void onStart() {

    }

    @Override
    public void onGetFileNum(int i) {

    }

    @Override
    public void onProgress(String dirName, String name, long size) {
        extractFile = new File(dirName, name);
        // >>10相当于除以1024取整 忽略1kb以下字节
        StringBuilder build = new StringBuilder("开始解压:").append(dirName).append('/').append(name).append(" , 解压后大小:").append(size >> 10).append("kb");
        Log.d(AssetsSoLoadBy7zFileManager.DIR_JNI_LIBS, build.toString());
        time = SystemClock.elapsedRealtime();
    }

    private void clearDirOtherFile(File dir, String current) {
        String[] children = dir.list();
        //递归删除目录中的其他so库
        for (int i = 0; i < children.length; i++) {
            String childName = children[i];
            if (TextUtils.equals(current, childName)) {
                continue;
            }
            File file = new File(dir, childName);
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    @Override
    public void onError(int i, String s) {
        endTime(false);
    }

    @Override
    public void onSucceed() {
        endTime(true);
        onSingleFileExtractSucceed(extractFile);
        //清理该文件夹下其他版本so库 TODO 后续异步清理
//        clearDirOtherFile(extractFile.getParentFile(), extractFile.getName());

    }

    private void endTime(boolean success) {
        long l = SystemClock.elapsedRealtime() - time;
        sumTime += l;
        StringBuilder builder = new StringBuilder(success ? "解压完成:" : "解压失败:").append(extractFile.getAbsolutePath())//
                .append(" , 耗时(毫秒):").append(l).append(" , 解压总计耗时(毫秒):").append(sumTime);
        Log.d(AssetsSoLoadBy7zFileManager.DIR_JNI_LIBS, builder.toString());
    }

    public abstract void onSingleFileExtractSucceed(File extractFile);
}