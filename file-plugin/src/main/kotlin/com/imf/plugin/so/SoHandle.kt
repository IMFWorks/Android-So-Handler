package com.imf.plugin.so

import com.android.utils.FileUtils
import com.android.ide.common.internal.WaitableExecutor
import com.elf.ElfParser
import org.json.simple.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors


class SoHandle(variantName: String, val extension: SoFileExtensions, val assetsOutDestManager: AssetsOutDestManager) {
    var saveJson: JSONObject? = null
    val assetsOutDestFile: File
    val cmd: String

    init {
        cmd = "${extension.exe7zName} a %s %s -t7z -mx=9 -m0=LZMA2 -ms=10m -mf=on -mhc=on -mmt=on -mhcf"
        assetsOutDestFile = assetsOutDestManager.assetsOutDestFile
    }


    fun perform7z(inputLibDir: File, executor: WaitableExecutor, transformLib: File?) {
        if (inputLibDir.exists() && inputLibDir.isDirectory()) {
            val abis = inputLibDir.listFiles()
            for (abi in abis) {
                if (!abi.isDirectory || abi.list().isEmpty()) {
                    break
                }
                if (isRetainAllSoFileByABIDir(abi)) {
                    if (saveJson == null) {
                        saveJson = JSONObject()
                    }
                    val recordMap = ConcurrentHashMap<String, HandleSoFileInfo>()
                    saveJson!!.put(abi.name, recordMap)
                    executor.execute({
                        handleSoFileByABIDir(abi,
                                if (transformLib != null) File(transformLib, abi.name) else null,
                                assetsOutDestFile,
                                extension.deleteSoLibs,
                                extension.compressSo2AssetsLibs, recordMap)
                    })
                }
            }

        }
    }


    //处理so库,压缩,删除等操作
    private fun handleSoFileByABIDir(abiDir: File, transformLibDest: File?, assetsAbiDir: File, deleteSoLibs: Set<String>?, compressSo2AssetsLibs: Set<String>?, recordMap: ConcurrentHashMap<String, HandleSoFileInfo>) {
        if (!abiDir.exists() || !abiDir.isDirectory) {
            log("${abiDir.absolutePath}不是文件夹")
            return
        }
        if (deleteSoLibs.isNullOrEmpty() && compressSo2AssetsLibs.isNullOrEmpty()) {
            log("没有要处理(删除|压缩)的SO库")
            FileUtils.copyDirectory(abiDir, transformLibDest)
            return
        }
        //transform中处理so时不复制到out目录则为删除不需要额外删除
        val needDeleteInputSo = if (transformLibDest == null) {
            true
        } else {
            if (!transformLibDest.exists()) {
                transformLibDest.mkdirs()
            }
            false
        }
        if (transformLibDest?.exists() ?: false) {
            transformLibDest?.mkdirs()
        }
        if (!assetsAbiDir.exists()) {
            assetsAbiDir.mkdirs()
        }
        abiDir.listFiles().toList().stream().filter({
            //处理压缩
            val result = compressSo2AssetsLibs?.contains(it.name) ?: false
            if (result) {
                val parseNeededDependencies = getNeededDependenciesBySoFile(abiDir, it, deleteSoLibs, compressSo2AssetsLibs)
                compressSoFileToAssets(it, File(assetsAbiDir, abiDir.name), parseNeededDependencies, recordMap, needDeleteInputSo)
            }
            !result
        }).filter({
            //处理删除
            val name = it.name
            val result = deleteSoLibs?.contains(name) ?: false
            if (result) {
                val parseNeededDependencies = getNeededDependenciesBySoFile(abiDir, it, deleteSoLibs, compressSo2AssetsLibs)
                recordMap[unmapLibraryName(name)] = HandleSoFileInfo(false, getFileMD5ToString(it), parseNeededDependencies, null)
                if (needDeleteInputSo) it.delete()
            }
            !result
        }).forEach { file: File ->
            //其余只是遍历得到依赖
            val parseNeededDependencies = getNeededDependenciesBySoFile(abiDir, file, deleteSoLibs, compressSo2AssetsLibs)
            if (!parseNeededDependencies.isNullOrEmpty()) {
                recordMap[unmapLibraryName(file.name)] = HandleSoFileInfo(false, null, parseNeededDependencies, null)
            }
            transformLibDest?.let { FileUtils.copyFile(file, File(it, file.name)) }
        }
    }

    private val renameList = mutableSetOf<String>();

    //压缩操作
    private fun compressSoFileToAssets(src: File, assetsABIDir: File, dependencies: List<String>?, recordMap: ConcurrentHashMap<String, HandleSoFileInfo>, isDeletSo: Boolean = false) {
        val md5 = getFileMD5ToString(src)
        val newMd5File = File(src.parentFile, md5)
        var name = src.name
        if (src.renameTo(newMd5File)) {
            renameList.add(name)
            name = unmapLibraryName(name)
            val destFile = File(assetsABIDir, "${name}&${md5}.7z")
            val exeCmd = String.format(cmd, destFile.getAbsolutePath(), newMd5File.getAbsolutePath())
            Runtime.getRuntime().exec(exeCmd).waitFor()
            recordMap[name] = HandleSoFileInfo(true, md5, dependencies, destFile.name)
            if (isDeletSo) {
                newMd5File.delete()
            }
        }
    }

    private fun getNeededDependenciesBySoFile(abiDir: File, soFile: File, deleteSoLibs: Set<String>?, compressSo2AssetsLibs: Set<String>?): List<String>? {
        if (deleteSoLibs.isNullOrEmpty() && compressSo2AssetsLibs.isNullOrEmpty()) {
            return null;
        }
        var elfParser: ElfParser? = null
        var dependenciesSet: MutableSet<String> = HashSet()
        try {
            try {
                elfParser = ElfParser(soFile)
                dependenciesSet.addAll(elfParser.parseNeededDependencies())
            } catch (ignored: Exception) {
                log("so解析失败:${soFile.absolutePath}")
            } finally {
                elfParser?.close()
            }
        } catch (ignored: IOException) {
            // This a redundant step of the process, if our library resolving fails, it will likely
            // be picked up by the system's resolver, if not, an exception will be thrown by the
            // next statement, so its better to try twice.
        }
        //在不需要全部依赖下,尝试进行依赖简化
        log("是否全部保留依赖:${extension.neededRetainAllDependencies}------解析出的依赖:${dependenciesSet}")
        if (!extension.neededRetainAllDependencies) {
            if (!dependenciesSet.isNullOrEmpty()) {
                dependenciesSet = dependenciesSet.stream()
                        .filter {
                            (deleteSoLibs?.contains(it) ?: false)
                                    || (compressSo2AssetsLibs?.contains(it) ?: false)
                        }.filter {
                            renameList.contains(it) || File(abiDir, it).exists()
                        }.collect(Collectors.toSet())
            }
        }
        //扩展自定义依赖
        val key = soFile.name
        log("配置自定义依赖前:${dependenciesSet}----- key:${key},${extension.customDependencies?.get(key)}")
        if (extension.customDependencies?.containsKey(key) ?: false) {
            val custom: List<String>? = extension.customDependencies?.get(key)
            if (!custom.isNullOrEmpty()) {
                dependenciesSet.addAll(custom)
            }
        }
        //libxxx.so -> xxx
        return if (dependenciesSet.isNullOrEmpty()) {
            null
        } else {
            val stream = dependenciesSet.stream()
            if (extension.excludeDependencies.isNullOrEmpty()) {
                stream
            } else {
                stream.filter { !extension.excludeDependencies!!.contains(it) }
            }.map { unmapLibraryName(it) }.collect(Collectors.toList())
        }
    }

    private fun log(message: String) {
        println(message)
    }

    //liblog.so -> log
    fun unmapLibraryName(mappedLibraryName: String): String {
        return mappedLibraryName.substring(3, mappedLibraryName.length - 3)
    }

    //log -> liblog.so
    fun mapLibraryName(libraryName: String): String {
        return if (libraryName.startsWith("lib") && libraryName.endsWith(".so")) {
            // Already mapped
            libraryName
        } else "lib${libraryName}.so"
    }


    //根据abi确定是保留jni
    fun isRetainAllSoFileByABIDir(abiDir: File): Boolean {
        if (extension.abiFilters == null) {
            return true;
        }
        return extension.abiFilters?.contains(abiDir.name) ?: false
    }

    fun resultWriteToFile() {
        saveJson?.let {
            FileUtils.writeToFile(File(assetsOutDestFile, "info.json"), it.toJSONString())
            assetsOutDestManager.finishCompressed();
        }
        saveJson = null;
    }

}