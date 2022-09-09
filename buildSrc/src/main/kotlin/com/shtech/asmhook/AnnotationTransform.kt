package com.shtech.asmhook

import com.android.build.api.transform.QualifiedContent
import com.google.common.collect.ImmutableSet
import org.objectweb.asm.tree.ClassNode

/**
 * 根据注解获取所有需要hook的方法
 */
class AnnotationTransform : BaseTransform() {

    companion object {
        //项目中定义的注解
        private const val HOOK_ANNOTATION_NAME = "Lcom/shtech/asmhook/HookMethod;"

        //项目中使用注解的class
        private val hookClassNames = hashMapOf("com/shtech/asmhook/HookHelper" to 0)

        //要hook的所有方法
        val hookMethodInfos = ArrayList<HookMethodInfo>()
    }

    override fun getName(): String {
        return AnnotationTransform::class.java.simpleName
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? {
        return ImmutableSet.of(QualifiedContent.Scope.PROJECT, QualifiedContent.Scope.SUB_PROJECTS)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun needWeave(classNode: ClassNode): Boolean {
        return hookClassNames[classNode.name] != null
    }

    override fun transformClassNode(classNode: ClassNode) {
        //遍历所有方法
        classNode.methods.forEach { methodNode ->
            //遍历注解，匹配 HookMethod 注解
            methodNode.invisibleAnnotations?.forEach { annotationNode ->
                if (annotationNode.desc == HOOK_ANNOTATION_NAME) {
                    //解析注解参数
                    val valueMap = convertToMap(annotationNode.values)
                    if (valueMap.isNotEmpty()) {
                        val ownerClass = valueMap["ownerClass"]
                        val methodName = valueMap["methodName"]

                        if (!ownerClass.isNullOrEmpty()) {
                            val name = if (methodName.isNullOrEmpty()) methodNode.name else methodName
                            val info = HookMethodInfo(ownerClass, name, methodNode.desc)
                            hookMethodInfos.add(info)

                            println("AsmHook hook method info ${hookMethodInfos.size}: ${info.ownerClass} ${info.methodName} ${info.methodDesc}")
                        }
                    }
                }
            }
        }
    }

    private fun convertToMap(values: MutableList<Any>?): HashMap<String, String> {
        if (values == null || values.isEmpty()) {
            return hashMapOf()
        }

        val hashMap = HashMap<String, String>()

        var index = 0
        while (index < values.size) {
            if (values[index].toString() == "ownerClass") {
                index++
                if (index < values.size) {
                    hashMap["ownerClass"] = values[index].toString()
                }
            }

            if (values[index].toString() == "methodName") {
                index++
                if (index < values.size) {
                    hashMap["methodName"] = values[index].toString()
                }
            }

            index++
        }

        return hashMap
    }
}