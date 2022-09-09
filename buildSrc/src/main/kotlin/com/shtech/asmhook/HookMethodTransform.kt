package com.shtech.asmhook

import com.android.build.api.transform.QualifiedContent
import com.google.common.collect.ImmutableSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * 根据 AnnotationTransform 获取的方法进行 hook
 */
class HookMethodTransform : BaseTransform() {

    override fun getName(): String {
        return HookMethodTransform::class.java.simpleName
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? {
        return ImmutableSet.of(QualifiedContent.Scope.PROJECT, QualifiedContent.Scope.SUB_PROJECTS, QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun needWeave(classNode: ClassNode): Boolean {
        if (classNode.name.startsWith("android", true)
            || classNode.name.startsWith("kotlin", true)
            || classNode.name.startsWith("com/google", true)
            || classNode.name.startsWith("cn/shuhe/foundation/utils/HookHelper", true)) {
            return false
        }
        return true
    }

    override fun transformMethodNode(classNode: ClassNode, methodNode: MethodNode) {
        //遍历方法的所有指令/操作码
        methodNode.instructions?.iterator()?.forEach { insnNode ->
            if (insnNode is MethodInsnNode) {
                transformMethodInsnNode(classNode, methodNode, insnNode)
            }
        }
    }

    private fun transformMethodInsnNode(classNode: ClassNode, methodNode: MethodNode, methodInsnNode: MethodInsnNode) {
        //遍历要 hook 方法
        AnnotationTransform.hookMethodInfos.forEach { info ->
            //匹配方法owner、方法名
            //if (methodInsnNode.name == info.methodName && (methodInsnNode.owner == info.className || "java/lang/Object" == info.className)) {
            if (methodInsnNode.owner == info.className && methodInsnNode.name == info.methodName) {

                //静态方法desc相同，非静态方法去除第一个参数owner
                val desc = if (methodInsnNode.opcode == Opcodes.INVOKESTATIC) {
                    info.methodDesc
                } else {
                    info.methodDesc.replaceFirst(info.ownerClass, "")
                }

                //对比desc，防止错误hook重载方法
                if (desc == methodInsnNode.desc) {
                    val oriInfo = "--- ${methodInsnNode.owner}\n--- ${methodInsnNode.name}\n--- ${methodInsnNode.desc}"

                    methodInsnNode.owner = "cn/shuhe/foundation/utils/HookHelper"
                    methodInsnNode.desc = info.methodDesc
                    methodInsnNode.opcode = Opcodes.INVOKESTATIC

                    val hookedInfo = "+++ ${methodInsnNode.owner}\n+++ ${methodInsnNode.name}\n+++ ${methodInsnNode.desc}"
                    println("========== AsmHook hook ${classNode.name}.${methodNode.name}\n$oriInfo\n$hookedInfo")
                }
            }
        }
    }
}