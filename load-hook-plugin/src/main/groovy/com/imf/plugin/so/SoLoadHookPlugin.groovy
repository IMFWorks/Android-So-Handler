package com.imf.plugin.so

import org.gradle.api.Plugin
import org.gradle.api.Project

class SoLoadHookPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('SoLoadHookConfig', SoLoadHookExtensions)
        SoLoadHookExtensions pluginConfig = project.SoLoadHookConfig
        project.android.registerTransform(new SoLoadHookTransform(pluginConfig))
    }
}