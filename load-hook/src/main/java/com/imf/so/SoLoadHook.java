package com.imf.so;

/**
 * @Author: lixiaoliang
 * @Date: 2021/8/13 3:50 PM
 * @Description: so库加载代理
 */
public class SoLoadHook {
    public static SoLoadProxy DEFAULT_SYSTEM_LOAD = new DefaultSoLoadProxy();
    private static SoLoadProxy sSoLoadProxy = DEFAULT_SYSTEM_LOAD;

    public static void setSoLoadProxy(SoLoadProxy soLoadProxy) {
        if (soLoadProxy == null) {
            sSoLoadProxy = DEFAULT_SYSTEM_LOAD;
            return;
        }
        sSoLoadProxy = soLoadProxy;
    }

    public static void loadLibrary(String libName) {
        sSoLoadProxy.loadLibrary(libName);
    }

    public static void load(String filename) {
        sSoLoadProxy.load(filename);
    }
}