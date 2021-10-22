package com.imf.so.assets.load;

import com.imf.so.assets.load.bean.SoFileInfo;

import java.io.File;
import java.util.List;

/**
 * 下载监听
 * 如果有需要下载的so文件在初始化时,回调onNeedDownloadSoInfo
 * 使用SoFileInfo#insertOrUpdateCache(saveLibsDir,File)完成下载后插入缓存
 */
public interface NeedDownloadSoListener {
    void onNeedDownloadSoInfo(File saveLibsDir, List<SoFileInfo> list);
}
