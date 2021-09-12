package com.imf.plugin.so

import com.android.build.gradle.AppExtension
import com.android.utils.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.stream.Collectors

abstract class SoFilePlugin : Plugin<Project> {
    lateinit var intermediatesDir: File;
    lateinit var android: AppExtension;
    lateinit var pluginConfig: SoFileExtensions
    override fun apply(project: Project) {
        pluginConfig = project.extensions.create("SoFileConfig", SoFileExtensions::class.java)
        android = project.extensions.getByType(AppExtension::class.java)
        intermediatesDir = FileUtils.join(project.buildDir, "intermediates")
        project.afterEvaluate {
            afterProjectEvaluate(it);
        }
    }

    protected open fun afterProjectEvaluate(project: Project) {
        val defaultConfig = android.defaultConfig
        pluginConfig.abiFilters = defaultConfig.ndk.abiFilters
        val os = System.getenv("OS").toLowerCase()
        if (os.contains("windows")) {
            pluginConfig.exe7zName = "7z.exe"
        }
        val minSdkVersion: Int = defaultConfig.minSdkVersion?.apiLevel ?: 0
        pluginConfig.neededRetainAllDependencies = pluginConfig.forceNeededRetainAllDependencies
                ?: (minSdkVersion <= 23)
    }
}

class SoFileTransformPlugin : SoFilePlugin() {
    override fun apply(project: Project) {
        super.apply(project)
        android.registerTransform(SoFileTransform(pluginConfig, intermediatesDir))
    }
}

class SoFileAttachMergeTaskPlugin : SoFilePlugin() {
    override fun afterProjectEvaluate(project: Project) {
        super.afterProjectEvaluate(project)
        var buildTypes: MutableSet<String> = android.buildTypes.stream().map { it.name }.filter {
            if (pluginConfig.excludeBuildTypes.isNullOrEmpty()) {
                true
            } else {
                !pluginConfig.excludeBuildTypes!!.contains(it)
            }
        }.collect(Collectors.toSet())
        if (!buildTypes.isNullOrEmpty()) {
            val tasks = project.tasks
            buildTypes!!.forEach { variantName: String ->
                val upperCaseName = upperCase(variantName)
                val taskName = "merge${upperCaseName}NativeLibs"
                val mergeNativeLibsTask = tasks.findByName(taskName)
                if (mergeNativeLibsTask == null) {
                    tasks.forEach {
                        if (it.name.equals(taskName)) {
                            it.doLast(SoFileVariantAction(variantName, pluginConfig, intermediatesDir))
                        }
                    }
                } else {
                    mergeNativeLibsTask.doLast(SoFileVariantAction(variantName, pluginConfig, intermediatesDir))
                }
            }
        }
    }

    private fun upperCase(str: String): String = str.substring(0, 1).toUpperCase() + str.substring(1)
}

