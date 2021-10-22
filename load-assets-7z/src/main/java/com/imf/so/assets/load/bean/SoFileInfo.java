package com.imf.so.assets.load.bean;

import android.os.Build;
import android.text.TextUtils;

import com.imf.so.assets.load.utils.LoadUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
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

    public File obtainSoFileBySaveLibsDir(File saveLibsDir) {
        return new File(saveLibsDir, LoadUtils.joinPath(abi, libName, md5));
    }

    public boolean exists(File saveLibsDir) {
        return obtainSoFileBySaveLibsDir(saveLibsDir).exists();
    }

    /**
     * 按指定名称插入或更新缓存
     *
     * @param saveLibsDir
     * @param source
     * @return
     */
    public boolean insertOrUpdateCache(File saveLibsDir, File source) {
        File destination = obtainSoFileBySaveLibsDir(saveLibsDir);
        File parentFile = destination.getParentFile();
        if (parentFile.exists()) {
            if (destination.exists()) {
                destination.delete();
            }
        } else {
            parentFile.mkdirs();
        }
        try {
            copyFileUsingFileStreams(source, destination);
        } catch (IOException e) {
            if (destination.exists()) {
                destination.delete();
            }
            return false;
        }
        return true;
    }

    private static final void copyFileUsingFileStreams(File source, File dest) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            LoadUtils.closeStreams(input, output);
        }
    }

    public boolean needDownload(File saveLibsDir) {
        if (TextUtils.isEmpty(libName)) {
            return false;
        }
        if (TextUtils.isEmpty(md5)) {
            return false;
        }

        return !saveCompressToAssets && !exists(saveLibsDir);
    }

    @Override
    public String toString() {
        return "{" + "libName='" + libName + '\'' + ", abi='" + abi + '\'' + ", md5='" + md5 + '\'' + ", dependencies=" + dependencies + '}';
    }
}