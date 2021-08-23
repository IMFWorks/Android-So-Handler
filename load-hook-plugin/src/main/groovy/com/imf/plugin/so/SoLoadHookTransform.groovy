package com.imf.plugin.so

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.android.utils.FileUtils
import groovy.io.FileType

class SoLoadHookTransform extends Transform {
    private SoLoadHookExtensions extension

    SoLoadHookTransform(SoLoadHookExtensions extension) {
        this.extension = extension
    }

    @Override
    String getName() {//这个名称会用于生成的gradle task名称
        return "soLoadHook"
    }
    /**
     * 需要处理的数据类型，有两种枚举类型
     * CLASSES 代表处理的 java 的 class 文件，RESOURCES 代表要处理 java 的资源
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return [QualifiedContent.DefaultContentType.CLASSES]
    }

    /**
     * 指 Transform 要操作内容的范围，官方文档 Scope 有 7 种类型：
     * 1. EXTERNAL_LIBRARIES        外部库
     * 2. PROJECT                   项目内容
     * 3. PROJECT_LOCAL_DEPS        项目的本地依赖(本地jar)
     * 4. PROVIDED_ONLY             提供本地或远程依赖项
     * 5. SUB_PROJECTS              子项目。
     * 6. SUB_PROJECTS_LOCAL_DEPS   子项目的本地依赖项(本地jar)。
     * 7. TESTED_CODE               由当前变量(包括依赖项)测试的代码
     * @return
     */
    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {//是否支持增量。这里暂时不实现增量。
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, com.android.build.api.transform.TransformException, InterruptedException {
        if (!isIncremental) {
            outputProvider.deleteAll()
        }
        SoLoadClassModifier.configExclude(extension.excludePackage, extension.isSkipRAndBuildConfig)
        WaitableExecutor executor = WaitableExecutor.useGlobalSharedThreadPool()
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                File dir = directoryInput.file
                if (dir) {
                    executor.execute({
                        FileUtils.copyDirectory(directoryInput.file, dest)
                        HashMap<String, File> modifyMap = new HashMap<>()
                        /**遍历以.class扩展名结尾的文件*/
                        dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                            File classFile ->
                                File modified = SoLoadClassModifier.modifyClassFile(dir, classFile, context.getTemporaryDir())
                                if (modified != null) {
                                    /**key 为包名 + 类名，如：/cn/sensorsdata/autotrack/android/app/MainActivity.class*/
                                    String ke = classFile.absolutePath.replace(dir.absolutePath, "")
                                    modifyMap.put(ke, modified)
                                }
                        }
                        modifyMap.entrySet().each {
                            Map.Entry<String, File> en ->
                                File target = new File(dest.absolutePath + en.getKey())
                                if (target.exists()) {
                                    target.delete()
                                }
                                FileUtils.copyFile(en.getValue(), target)
                                en.getValue().delete()
                        }
                        return null
                    })
                }
            }

            /**遍历 jar*/
            input.jarInputs.each { JarInput jarInput ->
                executor.execute({
                    String destName = jarInput.file.name
                    def hexName = SoLoadClassModifier.getHexNameByFilePath(jarInput.file)
                    /** 获取 jar 名字*/
                    if (destName.endsWith(".jar")) {
                        destName = destName.substring(0, destName.length() - 4)
                    }
                    /** 获得输出文件*/
                    File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    def modifiedJar = SoLoadClassModifier.modifyJar(jarInput.file, context.getTemporaryDir(), true)
                    if (modifiedJar == null) {
                        modifiedJar = jarInput.file
                    }
                    FileUtils.copyFile(modifiedJar, dest)
                    return null
                })
            }

        }
        executor.waitForTasksWithQuickFail(true);
    }
}