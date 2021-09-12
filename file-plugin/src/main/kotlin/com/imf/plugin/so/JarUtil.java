package com.imf.plugin.so;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarUtil { // 创建类
    private void zip(String zipFileName, File inputFile) throws Exception {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(zipFileName)); // 创建ZipOutputStream类对象
        zip(out, inputFile, ""); // 调用方法
        System.out.println("压缩中…"); // 输出信息
        out.close(); // 将流关闭
    }

    private void zip(JarOutputStream out, File f, String base) throws Exception { // 方法重载
        if (f.isDirectory()) { // 测试此抽象路径名表示的文件是否是一个目录
            File[] fl = f.listFiles(); // 获取路径数组
            out.putNextEntry(new JarEntry(base + "/")); // 写入此目录的entry
            base = base.length() == 0 ? "" : base + "/"; // 判断参数是否为空
            for (int i = 0; i < fl.length; i++) { // 循环遍历数组中文件
                zip(out, fl[i], base + fl[i]);
            }
        } else {
            out.putNextEntry(new JarEntry(base)); // 创建新的进入点
            // 创建FileInputStream对象
            FileInputStream in = new FileInputStream(f);
            int b; // 定义int型变量
            System.out.println(base);
            while ((b = in.read()) != -1) { // 如果没有到达流的尾部
                out.write(b); // 将字节写入当前ZIP条目
            }
            in.close(); // 关闭流
        }
    }
}