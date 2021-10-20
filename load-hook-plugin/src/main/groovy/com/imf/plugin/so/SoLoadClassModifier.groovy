package com.imf.plugin.so

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Matcher

/**
 * @Author: lixiaoliang* @Date: 2021/8/13 3:02 PM
 * @Description:
 */
class SoLoadClassModifier {
    private static HashSet<String> exclude = new HashSet<>();
    private static boolean isSkipRAndBuildConfig = true
    private StringBuffer logBuffer;
    static {
        exclude = new HashSet<>()
        exclude.add('android.support.')
        exclude.add('androidx.')
        exclude.add('com.google.android.')
        exclude.add('com.imf.so.')
    }

    static configExclude(Set<String> addExcludeSet, boolean skipRAndBuildConfig) {
        if (addExcludeSet != null && !addExcludeSet.isEmpty()) {
            exclude.addAll(addExcludeSet)
        }
        isSkipRAndBuildConfig = skipRAndBuildConfig
    }

    /**截取文件路径的 md5 值重命名输出文件,因为可能同名,会覆盖*/
    static String getHexNameByFilePath(File file) {
        return DigestUtils.md5Hex(file.absolutePath).substring(0, 8)
    }

    protected static boolean isShouldModify(String className) {
        Iterator<String> iterator = exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.startsWith(packageName)) {
                return false
            }
        }
        if (isSkipRAndBuildConfig
                && (className.contains('.BuildConfig') ||
                className.endsWith('.R2$') ||
                className.endsWith('.R') ||
                className.endsWith('.R2') ||
                className.endsWith('.R$'))) {
            return false
        }
        return true
    }

    static File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        /**
         * 读取原 jar
         */
        def file = new JarFile(jarFile, false)
        /**
         * 设置输出到的 jar
         */
        def hexName = ""
        if (nameHex) {
            hexName = getHexNameByFilePath(jarFile)
        }
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = null
            try {
                inputStream = file.getInputStream(jarEntry)
            } catch (Exception e) {
                return null
            }
            String entryName = jarEntry.getName()
            if (entryName.endsWith(".DSA") || entryName.endsWith(".SF")) {
                //ignore
            } else {
                String className
                JarEntry jarEntry2 = new JarEntry(entryName)
                jarOutputStream.putNextEntry(jarEntry2)

                byte[] modifiedClassBytes = null
                byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
                if (entryName.endsWith(".class")) {
                    className = entryName.replace(Matcher.quoteReplacement(File.separator), ".").replace(".class", "")
                    if (isShouldModify(className)) {
                        modifiedClassBytes = modifyClass(sourceClassBytes)
                    }
                }
                if (modifiedClassBytes == null) {
                    modifiedClassBytes = sourceClassBytes
                }
                jarOutputStream.write(modifiedClassBytes)
                jarOutputStream.closeEntry()
            }
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }

    static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }

    static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        try {
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            if (isShouldModify(className)) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = modifyClass(sourceClassBytes)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace('.', '') + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    new FileOutputStream(modified).write(modifiedClassBytes)
                }
            } else {
                modified = null
            }
        } catch (Exception e) {
            e.printStackTrace()
            modified = null
        }
        return modified
    }

    private static byte[] modifyClass(byte[] input) {
        if (new String(input).contains(LoadLibraryVisitor.TARGET_FLAG)) {
            ClassReader reader = new ClassReader(input)
            //COMPUTE_MAXS会让我们免于手动计算局部变量数和方法栈大小
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            ClassVisitor cv = new LoadLibraryVisitor(writer)
            reader.accept(cv, ClassReader.SKIP_DEBUG)
            return writer.toByteArray()
            return input
        } else { //无需转换
            return input
        }
    }
}