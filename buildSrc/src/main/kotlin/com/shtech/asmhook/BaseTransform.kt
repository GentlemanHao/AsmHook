package com.shtech.asmhook

import com.android.build.api.transform.*
import com.android.utils.FileUtils
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.parallel.InputStreamSupplier
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.*
import java.lang.Exception
import java.util.jar.JarFile

abstract class BaseTransform : Transform() {

    @Throws(TransformException::class, InterruptedException::class, IOException::class)
    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        val outputProvider = transformInvocation.outputProvider
        val incremental = transformInvocation.isIncremental

        //非增量编译，删除旧的输出
        if (!incremental) {
            outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                processJarInput(jarInput, outputProvider, incremental)
            }

            input.directoryInputs.forEach { directoryInput ->
                processDirInput(directoryInput, outputProvider, incremental)
            }
        }
    }

    /**
     * 处理JarInput
     */
    @Throws(IOException::class)
    private fun processJarInput(jarInput: JarInput, outputProvider: TransformOutputProvider, incremental: Boolean) {
        val dest = outputProvider.getContentLocation(jarInput.file.absolutePath, jarInput.contentTypes, jarInput.scopes, Format.JAR)

        if (incremental) {
            jarInput.status?.let { status ->
                when (status) {
                    Status.NOTCHANGED -> {
                    }
                    Status.ADDED, Status.CHANGED -> transformJar(jarInput.file, dest)
                    Status.REMOVED -> if (dest.exists()) {
                        FileUtils.delete(dest)
                    }
                }
            }
        } else {
            transformJar(jarInput.file, dest)
        }
    }

    @Throws(IOException::class)
    private fun transformJar(input: File, dest: File) {
        //println("transformJar: " + input.absolutePath + " " + dest.absolutePath)

        val jarFile = JarFile(input)
        val parallelScatterZipCreator = ParallelScatterZipCreator()

        jarFile.entries().asSequence().forEach { jarEntry ->
            parallelScatterZipCreator.addArchiveEntry(JarArchiveEntry(jarEntry), InputStreamSupplier {
                var inputStream: InputStream? = null
                try {
                    inputStream = jarFile.getInputStream(jarEntry)
                    if (jarEntry.name.endsWith("class")) {
                        return@InputStreamSupplier ByteArrayInputStream(inputStream.parsing())
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return@InputStreamSupplier inputStream
                }
                return@InputStreamSupplier inputStream
            })
        }

        val fos = FileOutputStream(dest)
        val zipArchiveOutputStream = ZipArchiveOutputStream(fos)
        try {
            parallelScatterZipCreator.writeTo(zipArchiveOutputStream)
            zipArchiveOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            zipArchiveOutputStream.close()
        }
    }

    /**
     * 处理DirInput
     */
    @Throws(IOException::class)
    private fun processDirInput(directoryInput: DirectoryInput, outputProvider: TransformOutputProvider, incremental: Boolean) {
        val dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.mkdirs(dest)

        if (incremental) {
            val srcDirPath = directoryInput.file.absolutePath
            val destDirPath = dest.absolutePath

            directoryInput.changedFiles.forEach { (inputFile, nullAbleStatus) ->
                val destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
                val destFile = File(destFilePath)

                nullAbleStatus?.let { status ->
                    when (status) {
                        Status.NOTCHANGED -> {
                        }
                        Status.REMOVED -> if (destFile.exists()) {
                            FileUtils.delete(destFile)
                        }
                        Status.ADDED, Status.CHANGED -> {
                            if (!destFile.exists()) {
                                FileUtils.createFile(destFile, "")
                            }
                            transformSingleFile(inputFile, destFile)
                        }
                    }
                }
            }
        } else {
            transformDir(directoryInput.file, dest)
        }
    }

    @Throws(IOException::class)
    private fun transformDir(input: File, dest: File) {
        //println("transformDir: " + input.absolutePath + " " + dest.absolutePath)
        if (dest.exists()) {
            FileUtils.delete(dest)
        }
        FileUtils.mkdirs(dest)
        val srcDirPath = input.absolutePath
        val destDirPath = dest.absolutePath

        input.listFiles()?.forEach { file ->
            val destFilePath = file.absolutePath.replace(srcDirPath, destDirPath)
            val destFile = File(destFilePath)

            if (file.isDirectory) {
                transformDir(file, destFile)
            } else if (file.isFile) {
                transformSingleFile(file, destFile)
            }
        }
    }

    @Throws(IOException::class)
    private fun transformSingleFile(input: File, dest: File) {
        if (input.absolutePath.endsWith("class")) {
            var fis: FileInputStream? = null
            var fos: FileOutputStream? = null
            try {
                fis = FileInputStream(input)
                fos = FileOutputStream(dest)
                fos.write(fis.parsing())
            } catch (e: Exception) {
                e.printStackTrace()
                FileUtils.copyFile(input, dest)
            } finally {
                try {
                    fos?.close()
                    fis?.close()
                } catch (e: Exception) {
                }
            }
        } else {
            FileUtils.copyFile(input, dest)
        }
    }

    /**
     * 解析输入流
     */
    private fun InputStream.parsing(): ByteArray {
        //用于将 Java 类文件转换成 ClassVisitor 能访问的结构，支持 byte[]、InputStream、File Path 三种输入方式
        val classReader = ClassReader(this)

        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.SKIP_DEBUG)

        classNode.weave()

        //用于生成符合 JVM 规范的字节码文件
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)

        return classWriter.toByteArray()
    }

    /**
     * 修改Class文件
     */
    private fun ClassNode.weave() {
        if (!needWeave(this)) {
            return
        }

        transformClassNode(this)

        this.methods.forEach { methodNode ->
            transformMethodNode(this, methodNode)
        }
    }

    /**
     * 是否需要注入、修改字节码
     * 多用于指定需要解析的类，或过滤不必要的官方类，如android、kotlin
     */
    abstract fun needWeave(classNode: ClassNode): Boolean

    /**
     * 处理class节点
     */
    open fun transformClassNode(classNode: ClassNode) {}

    /**
     * 处理class节点中的所有method节点
     */
    open fun transformMethodNode(classNode: ClassNode, methodNode: MethodNode) {}
}