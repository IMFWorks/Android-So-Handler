# Android-So-Handler

接入方式

```groovy
//1.根build.gradle中加入
buildscript {
    repositories {
        maven { url "https://raw.githubusercontent.com/IMFWorks/Android-So-Handler/master/maven" }
        ...
    }
    dependencies {
        ...
        classpath "com.imf.so:load-hook-plugin:${SO_PLUGIN_VERSION}" 
        classpath "com.imf.so:file-plugin:${SO_PLUGIN_VERSION}"
        ...
    }
}
allprojects {
    repositories {
      ...
      maven { url "https://raw.githubusercontent.com/IMFWorks/Android-So-Handler/master/maven" }
      ...
    }
}

//2.app工程中加入
dependencies {
	...
  implementation "com.imf.so:load-assets-7z:${SO_PLUGIN_VERSION}"
  ...
}
apply plugin: 'SoFileConfig'
SoFileConfig {
    //设置debug下不删除与压缩so库
    excludeBuildTypes = ['debug']
    //设置要删除的so库
    deleteSoLibs = [
    ]
    //设置要压缩的库 注意libun7zip.so 为7z解压库不可压缩
  	//这里名字要是带有lib开头与.so结尾与apk中so库名称一致
    compressSo2AssetsLibs = [
      'libxxx.so'
    ]
}

apply plugin: 'SoLoadHookConfig'
SoLoadHookConfig {
		//是否跳过R文件与BuildConfig
		isSkipRAndBuildConfig = true
		//设置跳过的包名,跳过的包不去hook 修改后请先clean
		excludePackage = ['com.imf.test']
}
//3.初始化 
AssetsSoLoadBy7zFileManager.init(v.getContext());
```

> SO_PLUGIN_VERSION 目前版本 `0.0.1`

## 插件介绍

### 一、 SoLoadHook插件

1. 通过Hook `System.loadLibrary` 与 `System.load`实现加载转发具体步骤如下:

   * 通过`ASM`框架对Hook目标类进行字节码修改具体为 `System.loadLibrary` 与 `System.load`修改成`SoLoadHook.loadLibrary`与`SoLoadHook.load` 
   * `SoLoadHook`可设置`SoLoadProxy`完成对外代理

    > `SoLoadHook`有默认实现只是调用`System.loadLibrary` 与 `System.load`

2. 具体接入步骤如下:

* gradle配置

```groovy
//SO_PLUGIN_VERSION 目前版本 0.0.1
//build.gradle中只加入
classpath "com.imf.so:load-hook-plugin:${SO_PLUGIN_VERSION}" 
//app.gradle中只配置
apply plugin: 'SoLoadHookConfig'
SoLoadHookConfig {
		//是否跳过R文件与BuildConfig
		isSkipRAndBuildConfig = true
		//设置跳过的包名,跳过的包不去hook 修改后请先clean
		excludePackage = ['com.imf.test']
}
dependencies {
  implementation "com.imf.so:load-hook:${SO_PLUGIN_VERSION}"
}
```

* java代码实现`SoLoadProxy`完成加载


```java
public interface SoLoadProxy {
    void loadLibrary(String libName);

    void load(String filename);
}
```

> 实现SoLoadProxy类后不会被修改字节码

### 二、SoFilePlugin插件依赖SoLoadHook

1. 通过transform插件对so库进行7z压缩(利用压缩差完成压缩apk),压缩后放入`asstes`下的`jniLib`
2. 根据压缩或删除so情况生成`info.json`
3. 运行时进行解压加载 so

> 1. so库依赖拷贝了[ReLinker](https://github.com/KeepSafe/ReLinker)中解析代码
> 2. 解压部分微调自[AndroidUn7z](https://github.com/hzy3774/AndroidUn7zip)

**接入方式参考顶部最开始部分**

