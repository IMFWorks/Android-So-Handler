package com.imf.plugin.so

//必须open 否则project.extensions.create无法创建SoFileExtensions的代理子类
open class SoFileExtensions {
    var exe7zName: String = "7z"
    //是否使用追加Action方式执行so压缩
    var useAppendActionMethod = true;
    var abiFilters: Set<String>? = null

    //要移除的so库
    var deleteSoLibs: Set<String>? = null

    //压缩放在assets下的so库
    var compressSo2AssetsLibs: Set<String>? = null
    var excludeBuildTypes: Set<String>? = null

    var excludeDependencies: Set<String>? = null


    /**
     * 是否需要保留所有依赖项
     * 默认为保留所有只保留删除或者压缩的依赖
     * minSdkVersion小于23则需要保留
     * 如果minSdkVersion大于23则不需要
     * 不可手动设置
     */
    var neededRetainAllDependencies: Boolean = true

    //强制保留所有依赖 对于minSdkVersion大于23的工程也保留所有依赖
    var forceNeededRetainAllDependencies: Boolean? = null

    /**
     * 配置自定义依赖
     * 用于解决 a.so 并未声明依赖 b.so 并且内部通过dlopen打开b.so
     * 或者反射System.loadLibrary等跳过hook加载so库等场景
     */
    var customDependencies: Map<String, List<String>>? = null
}