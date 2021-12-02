#-verbose #打印混淆的详细信息
#-optimizationpasses 5               #指定代码的压缩级别
#-dontoptimize                        #不进行优化，建议使用此选项，
#-dontpreverify                       #不进行预校验,Android不需要,可加快混淆速度。
#-ignorewarnings                      #忽略警告

#-dontshrink #声明不进行压缩操作，默认情况下，除了-keep配置（下详）的类及其直接或间接引用到的类，都会被移除。


-keep class com.hzy.lib7z.Z7Extractor {
    public native <methods>;
}

-keep interface com.hzy.lib7z.IExtractCallback {
    <methods>;
}
