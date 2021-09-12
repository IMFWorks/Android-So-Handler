package com.imf.plugin.so

class HandleSoFileInfo(val saveCompressToAssets: Boolean, val md5: String?, val dependencies: List<String>?, val compressName: String?) {
    //用于生成json
    override fun toString(): String {
        val s = StringBuilder("{\"saveCompressToAssets\":${saveCompressToAssets}")
        if (!dependencies.isNullOrEmpty()) {
            s.append(",\"dependencies\":[\"${dependencies[0]}\"")
            for (index in 1 until dependencies.size) {
                s.append(",\"${dependencies[index]}\"")
            }
            s.append(']')
        }
        md5?.let {
            s.append(",\"md5\":\"${md5}\"")
        }
        compressName?.let {
            s.append(",\"compressName\":\"${compressName}\"")
        }
        return s.append('}').toString()
    }
}