package com.imf.plugin.so

import com.android.utils.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class AssetsOutDestManager(val variantName: String, val intermediatesDir: File) {
    val ASSETS_ROOT = "merged_assets"
    val COMPRESSED_ASSETS = "compressed_assets"
    val JNI_LIBS = "jniLibs"
    val assetsOutDestFile: File by lazy {
        buildAssetsOutDestFile()
    }


    //debug -> mergeDebugAssets/out/jniLibs
    private fun buildAssetsOutDestFile(): File {
        var outDir = FileUtils.join(intermediatesDir, ASSETS_ROOT, variantName, "out")
        if (outDir.exists()) {
            return File(outDir, JNI_LIBS)
        }
        //老版本目录
        outDir = FileUtils.join(intermediatesDir, ASSETS_ROOT, variantName, "merge${variantName.substring(0, 1).toUpperCase() + variantName.substring(1)}Assets", "out")
        if (outDir.exists()) {
            return File(outDir, JNI_LIBS)
        } else {
            outDir.mkdirs()
            System.err.println("-----  未找到So库 Assete 输出目录  -----")
        }
        return File(outDir, JNI_LIBS)
    }

    fun finishCompressed() {
        //TODO 后续针对4.1.1 compressed_assets机制进行适配
//        //build\intermediates\compressed_assets\debug\out
//        val out = FileUtils.join(intermediatesDir, COMPRESSED_ASSETS, variantName, "out")
//        if (out.exists()) {
//            val assets = File(out, "assets")
//            if (!assets.exists()) {
//                assets.mkdirs()
//            }
//            val jniLibsDir = File(assets, JNI_LIBS)
//            val soJniLibsDir = FileUtils.join(intermediatesDir, COMPRESSED_ASSETS, variantName, "so", "assets")
//            if (assetsOutDestFile.exists() && assetsOutDestFile.isDirectory) {
////                val list = assetsOutDestFile.listFiles()
////                val assetsJniLibsDirPath = "assets${File.separatorChar}$JNI_LIBS"
////                for (abiLibs in list) {
////                    val abiDirName = abiLibs.name
////                    if (abiLibs.isDirectory) {
////                        val outDir = File(soJniLibsDir, abiDirName)
////                        if (!outDir.exists()) {
////                            outDir.mkdirs()
////                        }
////                        val path = "$assetsJniLibsDirPath${File.separatorChar}$abiDirName"
////                        abiLibs.listFiles().forEach {
////                            createJarByFile(path, it, File(outDir, "${it.name}.jar"))
////                        }
////                    } else {
////                        createJarByFile(assetsJniLibsDirPath, abiLibs, File(soJniLibsDir, "${abiLibs.name}.jar"))
////                    }
////                }
//                createJarByDir(File(jniLibsDir, "1.txt.jar"), assetsOutDestFile)
//            }
//
//        }
    }


    fun createJarByFile(path: String, inputFile: File, dest: File) {
        var inputStream: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        var outJarFile: JarOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(dest)
            outJarFile = JarOutputStream(fileOutputStream)
//            outJarFile.putNextEntry(JarEntry("$path/"))
            outJarFile.putNextEntry(JarEntry("$path${File.separatorChar}${inputFile.name}"))

            var b: Int = -1
            inputStream = FileInputStream(inputFile)
            while (inputStream.read().also { b = it } != -1) {
                outJarFile.write(b)
            }
        } catch (e: Exception) {
        } finally {
            outJarFile?.close()
            fileOutputStream?.close()
            inputStream?.close()
        }

    }

    fun createJarByDir(zipFileName: File, inputFile: File) {
        if (zipFileName.exists()) {
            zipFileName.delete()
        }
        val out = JarOutputStream(FileOutputStream(zipFileName)) // 创建ZipOutputStream类对象
        jar(out, inputFile, "assets") // 调用方法
        out.close() // 将流关闭
    }

    private fun jar(out: JarOutputStream, inputFile: File, path: String) {
        if (inputFile.isDirectory) {
            val input = inputFile.listFiles()
            out.putNextEntry(JarEntry("$path/"))
            for (i in input.indices) { // 循环遍历数组中文件
                jar(out, input[i], "$path${File.separatorChar}${input[i].name}")
            }
        } else {
            out.putNextEntry(JarEntry(path))
            var inputStream: FileInputStream? = null
            var b: Int = -1
            try {
                inputStream = FileInputStream(inputFile)
                while (inputStream.read().also { b = it } != -1) {
                    out.write(b)
                }
            } catch (e: Exception) {
            } finally {
                inputStream?.close()
            }

        }
    }
}