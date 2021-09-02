package com.imf.plugin.so

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import com.elf.ElfParser
import org.gradle.api.Plugin
import org.gradle.api.Project
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

class SoFilePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val pluginConfig: SoFileExtensions = project.extensions.create("SoFileConfig", SoFileExtensions::class.java)
        val android: AppExtension = project.extensions.getByType(AppExtension::class.java)

        android.registerTransform(SoFileTransform(pluginConfig, FileUtils.join(project.buildDir, "intermediates", "merged_assets")))
        project.afterEvaluate {
            val defaultConfig = android.defaultConfig
            pluginConfig.abiFilters = defaultConfig.ndk.abiFilters
            val minSdkVersion: Int = defaultConfig.minSdkVersion?.apiLevel ?: 0
            pluginConfig.neededRetainAllDependencies = pluginConfig.forceNeededRetainAllDependencies
                    ?: (minSdkVersion <= 23)
        }
    }
}

abstract class BaseLoggerTransform() : Transform() {
    private var isShowLog = false;
    fun logD(message: String) {
        if (isShowLog) {
            println("SoFilePlugin-DEBUG: ${message}")
        }

    }

    fun logI(message: String) {
        if (isShowLog) {
            println("SoFilePlugin-INFO: ${message}")
        }
    }


    override fun transform(transformInvocation: TransformInvocation?) {
        isShowLog = transformInvocation?.context?.variantName?.contains("debug", true) ?: false
        super.transform(transformInvocation)
    }
}

class SoFileTransform(val extension: SoFileExtensions, val mergedAssetsFile: File) : BaseLoggerTransform() {
    var saveJson: JSONObject? = null
    override fun getName(): String = "soFileTransform"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> = TransformManager.CONTENT_NATIVE_LIBS

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> = TransformManager.SCOPE_FULL_PROJECT;

    override fun isIncremental(): Boolean = false

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
        transformInvocation ?: return
        val outputProvider = transformInvocation.outputProvider
        if (!isIncremental) {
            outputProvider.deleteAll()
            saveJson = null
        }
        val variantName = transformInvocation.context.getVariantName()
        val assetsOutDestFile = buildAssetsOutDestFile(variantName)
        var isRetainAll = isRetainAllSoFileByVariantName(variantName)
        //如果没有配置删除或者压缩则保留全部
        if (!isRetainAll && extension.deleteSoLibs.isNullOrEmpty() && extension.compressSo2AssetsLibs.isNullOrEmpty()) {
            isRetainAll = true
        }
        println("isRetainAll:${isRetainAll}")
        val executor: WaitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
        transformInvocation.inputs.forEach { input: TransformInput ->
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
            renameList.add(name)
            name = unmapLibraryName(name)
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
        var dependenciesSet: MutableSet<String> = HashSet()
        try {
            try {
                elfParser = ElfParser(soFile)
                dependenciesSet.addAll(elfParser.parseNeededDependencies())
            } catch (ignored: Exception) {
                logD("so解析失败:${soFile.absolutePath}")
            } finally {
                elfParser?.close()
            }
        } catch (ignored: IOException) {
            // This a redundant step of the process, if our library resolving fails, it will likely
            // be picked up by the system's resolver, if not, an exception will be thrown by the
            // next statement, so its better to try twice.
        }
        //在不需要全部依赖下,尝试进行依赖简化
        logD("是否全部保留依赖:${extension.neededRetainAllDependencies}------解析出的依赖:${dependenciesSet}")
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
        logD("配置自定义依赖前:${dependenciesSet}----- key:${key},${extension.customDependencies?.get(key)}")
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