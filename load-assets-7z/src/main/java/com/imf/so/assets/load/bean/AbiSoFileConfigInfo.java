package com.imf.so.assets.load.bean;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author: lixiaoliang
 * @Date: 2021/10/22 11:37 AM
 * @Description: so库文件指定abi配置信息详情
 */
public class AbiSoFileConfigInfo {
    private final String mAbi;
    private final Map<String, SoFileInfo> mInfoMap;
    private final List<SoFileInfo> mNeedDownloadList;

    public static AbiSoFileConfigInfo tryLoadByJson(File saveLibsDir, String abi, JSONObject abiInfo) {
        if (abiInfo == null || abiInfo.length() <= 0 || TextUtils.isEmpty(abi)) {
            return null;
        }
        Iterator<String> keys = abiInfo.keys();
        Map<String, SoFileInfo> map = new HashMap<>(abiInfo.length());
        List<SoFileInfo> list = new ArrayList<>();
        while (keys.hasNext()) {
            String shortName = keys.next();
            SoFileInfo soFileInfo = SoFileInfo.fromJson(abiInfo, abi, shortName);
            if (soFileInfo != null) {
                map.put(shortName, soFileInfo);
                if (soFileInfo.needDownload(saveLibsDir)) {
                    list.add(soFileInfo);
                }
            }
        }
        return map.isEmpty() ? null : new AbiSoFileConfigInfo(abi, map, list);
    }

    private AbiSoFileConfigInfo(String abi, Map<String, SoFileInfo> infoMap, List<SoFileInfo> needDownloadList) {
        this.mAbi = abi;
        this.mInfoMap = infoMap;
        this.mNeedDownloadList = needDownloadList;
    }

    public String getAbi() {
        return mAbi;
    }

    public SoFileInfo getSoFileInfoByName(String libName) {
        return mInfoMap != null ? mInfoMap.get(libName) : null;
    }

    public List<SoFileInfo> getNeedDownloadList() {
        return mNeedDownloadList;
    }

    public boolean isNeedDownloadSo() {
        return mNeedDownloadList == null ? false : !mNeedDownloadList.isEmpty();
    }
}