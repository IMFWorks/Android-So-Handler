package com.imf.plugin.so

import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import org.gradle.api.Action
import java.io.File

/**
 * 新版本无法通过ExtendedContentType.NATIVE_LIBS获取so处理
 * 手动处理 在merge[Variant]NativeLibs中添加Action处理
 */
class SoFileVariantAction(val variantName: String, val extension: SoFileExtensions, val intermediatesDir: File) : Action<Any?> {

    override fun execute(input: Any?) {
        val soHandle = SoHandle(variantName, extension, AssetsOutDestManager(variantName, intermediatesDir))
        val mergedNativeLibsFile = buildMergedNativeLibsFile(variantName)
        val executor: WaitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
        soHandle.perform7z(mergedNativeLibsFile, executor, null)
        executor.waitForTasksWithQuickFail<Any?>(true);
        soHandle.resultWriteToFile()
    }

    //debug -> intermediates\merged_native_libs\debug\out\lib
    private fun buildMergedNativeLibsFile(variantName: String): File {
        return FileUtils.join(intermediatesDir, "merged_native_libs", variantName, "out", "lib")
    }

}