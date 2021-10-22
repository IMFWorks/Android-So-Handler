package com.imf.so.assets.load;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;


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

    public static boolean init(Context context, NeedDownloadSoListener listener) {
        if (sSaveLibsDir == null) {
            sSaveLibsDir = context.getDir(DIR_JNI_LIBS, Context.MODE_PRIVATE);
            //读取进程名称
            LoadUtils.getCurrentProcessNameByContext(context);

            JSONObject jsonObject = loadAssetsConfigJson(context);
            if (jsonObject == null) {
                Log.e(DIR_JNI_LIBS, "读取配置信息错误,导致初始化失败");
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
                Log.e(DIR_JNI_LIBS, "so配置abi,不支持该平台");
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
            Log.d(DIR_JNI_LIBS, "重复加载" + libName);
            return;
        }
        LogUtil.printDebug(" 进程:", LoadUtils.getCurrentProcessByCache(), " so库名称:", libName, " 是否是主线程:", Boolean.toString(LoadUtils.isMainThread()));
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
            LogUtil.printDebug("*********获得锁开始加载:", libSoInfo.libName);
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
            LogUtil.printDebug("获取到已存在加载锁:", libSoInfo.libName);
            return sLoadingLibraries.get(libSoInfo.libName);
        } else {
            Object loadingLibLock = new Object();
            sLoadingLibraries.put(libSoInfo.libName, loadingLibLock);
            LogUtil.printDebug("+++创建加载锁:", libSoInfo.libName);
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
        AssetManager assets = sAppContext.getAssets();
//        //输出解压日志
//        try {
//            InputStream open = assets.open(assetsFile);
//            int available = open.available();
//            Log.d(DIR_JNI_LIBS, "准备解压:" + assetsFile + " , 解压前大小: " + (available >> 10) + "kb");
//            open.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        LogUtil.printDebug("*********解压加载:", libSoInfo.libName);
        Z7Extractor.extractAsset(assets, assetsFile, outPath, new SingleSoFileExtractAndLoad(libSoInfo.libName));
    }

}