package com.imf.so.assets.load.bean;

import android.text.TextUtils;

import com.imf.so.assets.load.utils.LoadUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/19 4:08 PM
 * @Description:
 */
public class SoFileInfo {
    public String libName;
    public String abi;
    public boolean saveCompressToAssets;
    public String md5;
    public String compressName;
    public List<String> dependencies;

    private SoFileInfo(String libName, String abi, boolean saveCompressToAssets, String md5, String compressName, List<String> dependencies) {
        this.libName = libName;
        this.abi = abi;
        this.saveCompressToAssets = saveCompressToAssets;
        this.md5 = md5;
        this.compressName = compressName;
        this.dependencies = dependencies;
    }


    public static SoFileInfo fromJson(JSONObject json, String abi, String libName) {
        if (json == null) {
            return null;
        }
        if (json != null) {
            JSONObject targetSoInfo = json.optJSONObject(libName);
            if (targetSoInfo == null) {
                return null;
            }
            String md5 = targetSoInfo.optString("md5", null);
            if (TextUtils.isEmpty(md5)) {
                return null;
            }
            Object object = targetSoInfo.opt("saveCompressToAssets");
            if (object == null) {
                return null;
            }
            boolean saveCompressToAssets = LoadUtils.object2Boolean(object);
            String compressName = targetSoInfo.optString("compressName", null);
            JSONArray dependenciesJSONArray = targetSoInfo.optJSONArray("dependencies");
            List<String> dependencies = null;
            if (dependenciesJSONArray != null && dependenciesJSONArray.length() > 0) {
                dependencies = new ArrayList<>(dependenciesJSONArray.length());
                for (int j = 0; j < dependenciesJSONArray.length(); j++) {
                    String item = dependenciesJSONArray.optString(j);
                    if (!TextUtils.isEmpty(item)) {
                        dependencies.add(item);
                    }
                }
            }
            return new SoFileInfo(libName, abi, saveCompressToAssets, md5, compressName, dependencies);
        }
        return null;
    }


}