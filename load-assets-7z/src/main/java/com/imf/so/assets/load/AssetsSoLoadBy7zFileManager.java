package com.imf.so.assets.load;

import android.content.Context;
import android.text.TextUtils;


import com.imf.so.SoLoadHook;
import com.hzy.lib7z.Z7Extractor;
import com.imf.so.assets.load.bean.SoFileInfo;
import com.imf.so.assets.load.extract.SingleSoFileExtractAndLoad;
import com.imf.so.assets.load.utils.LoadRecordHelp;
import com.imf.so.assets.load.utils.LoadUtils;
import com.imf.so.assets.load.utils.LogUtil;
import com.imf.so.assets.load.bean.AbiSoFileConfigInfo;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/19 2:59 PM
 * @Description:
 */
public class AssetsSoLoadBy7zFileManager {
    public final static String DIR_JNI_LIBS = "jniLibs";
    public final static String ASSETS_CONFIG_INFO_PATH = "jniLibs/info.json";
    private static Context sAppContext;
    private static File sSaveLibsDir;
    private static AbiSoFileConfigInfo sSoLoadInfo;

    private static final Map<String, Object> sLoadingLibraries = new HashMap<>();

    public static void setLogEnable(boolean enable) {
        LogUtil.setLogEnable(enable);
    }

    public static boolean init(Context context, NeedDownloadSoListener listener) {
        if (sSaveLibsDir == null) {
            sSaveLibsDir = context.getDir(DIR_JNI_LIBS, Context.MODE_PRIVATE);
            //读取进程名称
            LoadUtils.getCurrentProcessNameByContext(context);

            JSONObject jsonObject = loadAssetsConfigJson(context);
            if (jsonObject == null) {
                LogUtil.printError(true, "读取配置信息错误,导致初始化失败");
                return false;
            }
            String[] supportedAbis = LoadUtils.supportedAbis();
            for (int i = 0; i < supportedAbis.length; i++) {
                String abi = supportedAbis[i];
                JSONObject abiInfo = jsonObject.optJSONObject(abi);
                if (abiInfo != null) {
                    //这里只需确定支持的即可 系统不允许加载不同架构so文件
                    sSoLoadInfo = AbiSoFileConfigInfo.tryLoadByJson(sSaveLibsDir, abi, abiInfo);
                    break;
                }
            }
            if (sSoLoadInfo == null) {
                LogUtil.printError(true, "so配置abi,不支持该平台");
                return false;
            }
            sAppContext = context.getApplicationContext();
            //设置加载代理
            SoLoadHook.setSoLoadProxy(new AssetsSoLoadBy7z());
            if (listener != null && sSoLoadInfo.isNeedDownloadSo()) {
                listener.onNeedDownloadSoInfo(sSaveLibsDir, sSoLoadInfo.getNeedDownloadList());
            }
        }
        return true;
    }

    public static boolean init(Context context) {
        return init(context, null);
    }


    /**
     * 读取assets中配置信息详情
     *
     * @param context
     * @return 返回读取是否成功
     */
    private static JSONObject loadAssetsConfigJson(Context context) {
        try {
            String infoJson = LoadUtils.loadStringByInputStream(context.getAssets().open(ASSETS_CONFIG_INFO_PATH));
            return new JSONObject(infoJson);
        } catch (Exception e) {
        }
        return null;
    }


    /**
     * loadLibrary so库加载入口
     *
     * @param libName
     */
    public static void loadLibraryAndDependencies(String libName) {
        if (LoadRecordHelp.isLoaded(libName)) {
            LogUtil.printDebug("重复加载" + libName);
            return;
        }
        SoFileInfo soFileInfoByName = sSoLoadInfo.getSoFileInfoByName(libName);
        if (soFileInfoByName != null) {
            loadBySoFileInfo(soFileInfoByName);
        } else {
            LoadRecordHelp.loadSoLibrary(libName);
        }
    }

    /**
     * 按配置加载
     *
     * @param libSoInfo
     */
    private static void loadBySoFileInfo(SoFileInfo libSoInfo) {
        if (libSoInfo.dependencies != null && !libSoInfo.dependencies.isEmpty()) {
            for (String dependency : libSoInfo.dependencies) {
                loadLibraryAndDependencies(dependency);
            }
        }
        synchronized (getLoadingLibLock(libSoInfo)) {
            File source = libSoInfo.obtainSoFileBySaveLibsDir(sSaveLibsDir);
            //已经解压过,存在so文件直接加载
            if (source.exists()) {
                LoadRecordHelp.loadSoFile(source.getAbsolutePath(), libSoInfo.libName);
            } else if (libSoInfo.saveCompressToAssets && !TextUtils.isEmpty(libSoInfo.compressName)) {//解压加载
                loadByExtractAsset(libSoInfo, source);
            } else {//尝试正常加载 逻辑上不会触发到else
                LoadRecordHelp.loadSoLibrary(libSoInfo.libName);
            }
        }
    }

    private static synchronized Object getLoadingLibLock(SoFileInfo libSoInfo) {
        if (sLoadingLibraries.containsKey(libSoInfo.libName)) {
            return sLoadingLibraries.get(libSoInfo.libName);
        } else {
            Object loadingLibLock = new Object();
            sLoadingLibraries.put(libSoInfo.libName, loadingLibLock);
            return loadingLibLock;
        }
    }


    /**
     * 解压至指定文件加载
     *
     * @param libSoInfo
     * @param source
     */
    private static void loadByExtractAsset(SoFileInfo libSoInfo, File source) {
        String assetsFile = LoadUtils.joinPath(DIR_JNI_LIBS, libSoInfo.abi, libSoInfo.compressName);
        String outPath = source.getParent();
        Z7Extractor.extractAsset(sAppContext.getAssets(), assetsFile, outPath, new SingleSoFileExtractAndLoad(libSoInfo.libName));
    }

}