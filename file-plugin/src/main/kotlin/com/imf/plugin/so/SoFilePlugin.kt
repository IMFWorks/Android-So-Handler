package com.imf.plugin.so

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import com.elf.ElfParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.json.simple.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

//必须open 否则project.extensions.create无法创建SoFileExtensions的代理子类
open class SoFileExtensions {
    var abiFilters: Set<String>? = null

    //要移除的so库
    var deleteSoLibs: Set<String>? = null

    //压缩放在assets下的so库
    var compressSo2AssetsLibs: Set<String>? = null
    var excludeBuildTypes: Set<String>? = null
}

class SoFilePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val pluginConfig: SoFileExtensions = project.extensions.create("SoFileConfig", SoFileExtensions::class.java)
        val android: AppExtension = project.extensions.getByType(AppExtension::class.java)

        android.registerTransform(SoFileTransform(pluginConfig, FileUtils.join(project.buildDir, "intermediates", "merged_assets"), project.logger))
        project.afterEvaluate { pluginConfig.abiFilters = android.defaultConfig.ndk.abiFilters }
    }
}

abstract class BaseLoggerTransform(private val log: Logger) : Transform() {

    fun logD(message: String) {
        log.log(LogLevel.DEBUG, "SoFilePlugin: ${message}")
    }

    fun logI(message: String) {
        log.log(LogLevel.INFO, "SoFilePlugin: ${message}")
    }

}

class SoFileTransform(val extension: SoFileExtensions, val mergedAssetsFile: File, log: Logger) : BaseLoggerTransform(log) {
    var saveJson: JSONObject? = null
    override fun getName(): String = "soFileTransform"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> = TransformManager.CONTENT_NATIVE_LIBS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> = TransformManager.SCOPE_FULL_PROJECT;

    override fun isIncremental(): Boolean = false

    override fun transform(context: Context, inputs: MutableCollection<TransformInput>, referencedInputs: MutableCollection<TransformInput>, outputProvider: TransformOutputProvider, isIncremental: Boolean) {
        if (!isIncremental) {
            outputProvider.deleteAll()
            saveJson = null
        }
        val variantName = context.getVariantName()
        val assetsOutDestFile = buildAssetsOutDestFile(variantName)
        val isRetainAll = isRetainAllSoFileByVariantName(variantName)
        println("isRetainAll:${isRetainAll}")
        val executor: WaitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
        inputs.forEach { input: TransformInput ->
            input.directoryInputs.forEach { directoryInput: DirectoryInput ->
                val dest: File = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                val dir: File = directoryInput.file
                if (dir.exists() && dir.isDirectory) {
                    if (isRetainAll) {
                        FileUtils.copyDirectory(directoryInput.file, dest)
                    } else {
                        val lib: File = File(dir, "lib")
                        val transformLib: File = File(dest, "lib")
                        if (lib.exists() && lib.isDirectory()) {
                            val abis = lib.listFiles()
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
                                                File(transformLib, abi.name),
                                                assetsOutDestFile,
                                                extension.deleteSoLibs,
                                                extension.compressSo2AssetsLibs, recordMap)

                                    })
                                }
                            }

                        }

                    }
                }
            }
        }
        executor.waitForTasksWithQuickFail<Any?>(true);
        saveJson?.let {
            FileUtils.writeToFile(File(assetsOutDestFile, "info.json"), it.toJSONString())
        }
    }

    //debug -> mergeDebugAssets/out/jniLibs
    private fun buildAssetsOutDestFile(variantName: String) =
            FileUtils.join(mergedAssetsFile, variantName, "merge${variantName.substring(0, 1).toUpperCase() + variantName.substring(1)}Assets", "out", "jniLibs")


    //根据编译模式确定是否全部保留
    fun isRetainAllSoFileByVariantName(variantName: String): Boolean {
        return extension.excludeBuildTypes?.contains(variantName) ?: false
    }

    //根据abi确定是保留jni
    fun isRetainAllSoFileByABIDir(abiDir: File): Boolean {
        if (extension.abiFilters == null) {
            return true;
        }
        return extension.abiFilters?.contains(abiDir.name) ?: false
    }


    //处理so库,压缩,删除等操作
    fun handleSoFileByABIDir(abiDir: File, dest: File, assetsAbiDir: File, deleteSoLibs: Set<String>?, compressSo2AssetsLibs: Set<String>?, recordMap: ConcurrentHashMap<String, HandleSoFileInfo>) {
        if (!abiDir.exists() || !abiDir.isDirectory) {
            logD("${abiDir.absolutePath}不是文件夹")
            return
        }
        if (deleteSoLibs.isNullOrEmpty() && compressSo2AssetsLibs.isNullOrEmpty()) {
            logD("没有要处理(删除|压缩)的SO库")
            FileUtils.copyDirectory(abiDir, dest)
            return
        }
        if (!dest.exists()) {
            dest.mkdirs()
        }
        if (!assetsAbiDir.exists()) {
            dest.mkdirs()
        }
        abiDir.listFiles().toList().stream().filter({
            val result = compressSo2AssetsLibs?.contains(it.name) ?: false
            if (result) {
                val parseNeededDependencies = getNeededDependenciesBySoFile(abiDir, it, deleteSoLibs, compressSo2AssetsLibs)
                compressSoFileToAssets(it, File(assetsAbiDir, abiDir.name), parseNeededDependencies, recordMap)
            }
            !result
        }).filter({
            val result = deleteSoLibs?.contains(name) ?: false
            if (result) {
                val parseNeededDependencies = getNeededDependenciesBySoFile(abiDir, it, deleteSoLibs, compressSo2AssetsLibs)
                recordMap[unmapLibraryName(name)] = HandleSoFileInfo(false, getFileMD5ToString(it), parseNeededDependencies, null)
            }
            !result
        }).forEach { file: File ->
            val parseNeededDependencies = getNeededDependenciesBySoFile(abiDir, file, deleteSoLibs, compressSo2AssetsLibs)
            if (!parseNeededDependencies.isNullOrEmpty()) {
                recordMap[unmapLibraryName(file.name)] = HandleSoFileInfo(false, null, parseNeededDependencies, null)
            }
            FileUtils.copyFile(file, File(dest, file.name))
        }
    }

    private val renameList = mutableSetOf<String>();

    //压缩操作
    private fun compressSoFileToAssets(src: File, assetsABIDir: File, dependencies: List<String>?, recordMap: ConcurrentHashMap<String, HandleSoFileInfo>) {
        val md5 = getFileMD5ToString(src)
        val newMd5File = File(src.parentFile, md5)
        var name = src.name
        if (src.renameTo(newMd5File)) {
            name = unmapLibraryName(name)
            renameList.add(name)
            val destFile = File(assetsABIDir, "${name}&${md5}.7z")
            val cmd = "7z a ${destFile.getAbsolutePath()} ${newMd5File.getAbsolutePath()} -t7z -mx=9 -m0=LZMA2 -ms=10m -mf=on -mhc=on -mmt=on -mhcf"
            logD("cmd->${cmd}")
            Runtime.getRuntime().exec(cmd).waitFor()
            recordMap[name] = HandleSoFileInfo(true, md5, dependencies, destFile.name)
        }
    }

    private fun getNeededDependenciesBySoFile(abiDir: File, soFile: File, deleteSoLibs: Set<String>?, compressSo2AssetsLibs: Set<String>?): List<String>? {
        if (deleteSoLibs.isNullOrEmpty() && compressSo2AssetsLibs.isNullOrEmpty()) {
            return null;
        }
        var elfParser: ElfParser? = null
        var parseNeededDependencies: List<String>? = null
        try {
            try {
                elfParser = ElfParser(soFile)
                parseNeededDependencies = elfParser.parseNeededDependencies()
            } catch (ignored: Exception) {
                print("so解析失败:${soFile.absolutePath}")
            } finally {
                elfParser?.close()
            }
        } catch (ignored: IOException) {
            // This a redundant step of the process, if our library resolving fails, it will likely
            // be picked up by the system's resolver, if not, an exception will be thrown by the
            // next statement, so its better to try twice.
        }
        if (parseNeededDependencies?.isNotEmpty() ?: false) {
            return parseNeededDependencies!!.stream()
                    .filter {
                        (deleteSoLibs?.contains(it) ?: false)
                                || (compressSo2AssetsLibs?.contains(it) ?: false)
                    }.map {
                        unmapLibraryName(it)
                    }.filter {
                        renameList.contains(it) || File(abiDir, mapLibraryName(it)).exists()
                    }.collect(Collectors.toList())
        }
        return parseNeededDependencies
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


}

class HandleSoFileInfo(val saveCompressToAssets: Boolean, val md5: String?, val dependencies: List<String>?, val compressName: String?) {
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