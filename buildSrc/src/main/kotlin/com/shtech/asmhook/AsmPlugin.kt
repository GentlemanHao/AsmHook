package com.shtech.asmhook

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AsmPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("=============================")
        println("======= Hello AsmHook =======")
        println("=============================")

        //注入两个Transform，一个根据注解获取所有需要hook方法，一个实际hook上述所有方法
        val appExtension = project.extensions.findByType(AppExtension::class.java)

        AnnotationTransform.hookMethodInfos.clear()
        appExtension?.registerTransform(AnnotationTransform())

        appExtension?.registerTransform(HookMethodTransform())
    }
}