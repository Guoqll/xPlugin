package com.frame.plugin.flutter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.util.ui.ImageUtil
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.*

class FlutterAddIMG : AnAction("Flutter Add Img") {
    var project: Project? = null
    var psiDirectory: PsiDirectory? = null
    var lib: VirtualFile? = null
    var generated: VirtualFile? = null
    var a_file: VirtualFile? = null
    var assets: VirtualFile? = null
    var list = HashMap<String, String>()
    var psifile: PsiFile? = null
    var psifile2: PsiFile? = null
    var psifile3: PsiFile? = null
    var isFilter = false
    var jTextField_filter: JTextField? = null

    //file name
    var name = ""
    override fun actionPerformed(e: AnActionEvent) {
        project = e.getData(PlatformDataKeys.PROJECT)
        val psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT)
        if (psiElement is PsiFile) {
            Messages.showErrorDialog("just open with dir", "Attention!")
            return
        }
        psiDirectory = psiElement as PsiDirectory?


        //添加   批量  自动创建 2.0x 3.0x 的图片和文件夹, 至于自动生成pubspec的行数 再说  //批量
//        data.createSubdirectory("2.0x");
//        data.createSubdirectory("3.0x");
        if (project == null) {
            return
        }

        val virtualFile = project!!.workspaceFile!!.parent.parent
        if (virtualFile.isDirectory) {
            val children = virtualFile.children
            for (virtualFile1 in children) {
                if (virtualFile1.name == "lib") {
                    lib = virtualFile1
                }
                if (virtualFile1.name == "assets") {
                    assets = virtualFile1
                }
            }
            initR()
            addFilePath(assets)
        }

        EventQueue.invokeLater {
            //------------------
            val frame = JFrame()
            frame.title = "Flutter Add Image -- Drag File To Here ----> " + psiDirectory!!.name
            frame.setSize(500, 300)
            frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE
            frame.isVisible = true
            jTextField_filter = JTextField()

            //                frame.add(jTextField_filter);
            val checkBox = JCheckBox()
            checkBox.text = "Use Name Filter"
            checkBox.setSize(20, 20)
            //                frame.add(checkBox);
            checkBox.addChangeListener { e ->
                val checkBox = e.source as JCheckBox
                isFilter = checkBox.isSelected
            }
            val checkBox1 = JCheckBox()
            checkBox1.text = "always on top"
            checkBox1.setSize(20, 20)
            checkBox1.addChangeListener { e ->
                val checkBox = e.source as JCheckBox
                if (checkBox.isSelected) {
                    frame.isAlwaysOnTop = true
                } else {
                    frame.isAlwaysOnTop = false
                }
            }
            val jTextField = JTextField()
            jTextField.setSize(500, 200)
            jTextField.text = "Drag to here"
            jTextField.horizontalAlignment = SwingConstants.CENTER
            //                frame.add(jTextField);
            val panel = JPanel()
            panel.preferredSize = Dimension(500, 250)
            panel.add(jTextField_filter)
            panel.add(checkBox)
            panel.add(jTextField)
            panel.add(checkBox1)
            jTextField.isEditable = false
            jTextField.preferredSize = Dimension(500, 200)
            frame.contentPane = panel
            jTextField.transferHandler = object : TransferHandler() {
                private val serialVersionUID = 1L
                override fun importData(comp: JComponent, t: Transferable): Boolean {
                    try {
                        val o = t.getTransferData(DataFlavor.javaFileListFlavor)
                        var filepath = o.toString()
                        if (filepath.startsWith("[")) {
                            filepath = filepath.substring(1)
                        }
                        if (filepath.endsWith("]")) {
                            filepath = filepath.substring(0, filepath.length - 1)
                        }
                        if (filepath.endsWith("png") or filepath.endsWith("jpg") or filepath.endsWith("jpeg")) {
                        } else {
                            Messages.showErrorDialog("Just for Image", "Attention!")
                            return false
                        }


                        //filepath 拖拽的文件
                        genImages(File(filepath))
                        //路径
                        //                    System.out.println(filepath);
                        //                    field.setText(filepath);
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return false
                }

                override fun canImport(comp: JComponent, flavors: Array<DataFlavor>): Boolean {
                    for (i in flavors.indices) {
                        if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                            return true
                        }
                    }
                    return false
                }
            }
        }
    }

    var subdirectory2: PsiDirectory? = null
    var subdirectory3: PsiDirectory? = null

    fun genImages(file: File?) {
        if (file == null) {
            return
        }
        if (file.isDirectory) {
            Messages.showErrorDialog("Just for Image", "Attention!")
            return
        }
        try {
            WriteCommandAction.runWriteCommandAction(project) {
                subdirectory2 = psiDirectory!!.createSubdirectory("2.0x")
                subdirectory3 = psiDirectory!!.createSubdirectory("3.0x")
            }
        } catch (e: java.lang.Exception) {
            val subdirectories = psiDirectory!!.subdirectories
            subdirectory2 = psiDirectory!!.findSubdirectory("2.0x")
            subdirectory3 = psiDirectory!!.findSubdirectory("3.0x")
        }
        val selectedFile: File = file
        try {
            if (isFilter) {
                val text = jTextField_filter!!.text
                if (text == null) {
                    Messages.showErrorDialog("Please enter content", "Attention!")
                    return
                }
                if ("" == text) {
                    Messages.showErrorDialog("Please enter content,or do not use filter", "Attention!")
                    return
                }
                name = selectedFile.name
                name = if (name.contains(text)) {
                    name.replace(text.toRegex(), "")
                } else {
                    Messages.showErrorDialog("必须包含被过滤文本", "Attention!")
                    return
                }
            } else {
                name = selectedFile.name
            }
            WriteCommandAction.runWriteCommandAction(project) { // x1
                psifile = psiDirectory!!.createFile(name)
                //x2
                psifile2 = subdirectory2!!.createFile(name)
                //x3
                psifile3 = subdirectory3!!.createFile(name)
            }
        } catch (e: java.lang.Exception) {
            Messages.showErrorDialog("File already exists", "Attention!")
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                flex(1f, selectedFile, psifile)
                flex(2f, selectedFile, psifile2)
                flex(3f, selectedFile, psifile3)
                list.clear()
                addFilePath(assets)
                gen_AContent()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    fun flex(num: Float, file: File, psifile: PsiFile?) {
        //1原来宽和高与新的宽高的比例
        val fis = FileInputStream(file)
        val read = ImageIO.read(fis)
        val width = read.width / 3 * num
        val height = read.height / 3 * num
        val image = read.getScaledInstance(
            width.toInt(), height.toInt(),
            Image.SCALE_SMOOTH
        )
        val outputImage = ImageUtil.toBufferedImage(image)
        val baos = ByteArrayOutputStream()
        ImageIO.write(outputImage, file.path.substring(file.path.lastIndexOf(".") + 1), baos)
        psifile?.virtualFile?.setBinaryContent(baos.toByteArray())
    }


    fun initR() {
        WriteCommandAction.runWriteCommandAction(project, Runnable {
            try {
                generated = lib!!.findChild("generated")
                if (generated == null) {
                    generated = lib!!.createChildDirectory(null, "generated")
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            if (generated == null) {
                return@Runnable
            }
            a_file = generated!!.findChild(R_DART_FILE)
            if (a_file == null) {
                PsiFileFactory.getInstance(project)
                val s = "class R{\n" +
                        " //auto gen ,do net edit !!! \n" +
                        "  static String test=\"test\";\n" +
                        "}"
                val initFile =
                    PsiFileFactory.getInstance(project).createFileFromText(R_DART_FILE, s)
                PsiManager.getInstance(project!!).findDirectory(generated!!)!!.add(initFile)
            } else {
            }
        })
    }

    val R_DART_FILE = "res.dart"

    //生成A文件的内容
    fun gen_AContent() {
        a_file = generated!!.findChild(R_DART_FILE)
        val sb = StringBuilder()
        val s = "class Res{\n" +
                " //auto gen ,do not edit! \n"
        sb.append(s)
        val iterator: Iterator<Map.Entry<String, String>> = list.entries.iterator()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            var newkey = StringBuilder()
            key.split("_").forEachIndexed { index, str ->
                if (index > 0) {
                    val firstStr = str.substring(0, 1)
                    val otherStr = str.subSequence(1, str.length)
                    newkey.append(firstStr.uppercase())
                    newkey.append(otherStr)
                } else {
                    newkey.append(str)
                }

            }
            sb.append("static const ${newkey.toString()} = \"$value\";\n")
        }
        sb.append("}")
        try {
            a_file!!.setBinaryContent(sb.toString().toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun addFilePath(f: VirtualFile?) {
        val files = f?.children
        files?.let {
            for (ff in files) {
                if (ff.isDirectory) {
                    if (ff.name == "2.0x") {
                        continue
                    }
                    if (ff.name == "3.0x") {
                        continue
                    }
                    addFilePath(ff)
                } else {
                    if (ff.path.endsWith("png") or ff.path.endsWith("jpg") or ff.path.endsWith("jpeg")) {
                        val path = ff.path
                        if(path.contains("assets/images/")){
                            val path1 = path.substring(path.indexOf("assets"))
                            var path2 = path1.replace("/".toRegex(), "_")
                            path2 = convertFilePathToCamelCase(path1)
                            list[path2] = path.substring(path.indexOf("assets"))
                        }
                    }
                }
            }
        }

    }

    fun convertFilePathToCamelCase(filePath: String): String {
        // 定义固定路径
        val fixedPath = "assets/images/"
        // 去掉固定路径
        var trimmedPath = filePath.replace(fixedPath, "")

        // 使用正则表达式检查并保留文件后缀（如png或jpg）
        val extensionRegex = Regex("\\.(png|jpg)$")
        val extensionMatch = extensionRegex.find(trimmedPath)

        // 获取文件后缀
        val fileExtension = extensionMatch?.groups?.get(1)?.value?.capitalize() ?: ""

        // 去掉文件后缀
        trimmedPath = trimmedPath.replace(extensionRegex, "")

        // 使用下划线分割字符串并转换为驼峰命名法
        val parts = trimmedPath.split("_")
        val camelCase = StringBuilder()

        for ((index, part) in parts.withIndex()) {
            if (index == 0) {
                // 第一个部分小写
                camelCase.append(part.toLowerCase())
            } else {
                // 其他部分首字母大写
                camelCase.append(part.capitalize())
            }
        }

        // 将文件后缀添加到结果的末尾
        if (fileExtension.isNotEmpty()) {
            camelCase.append(fileExtension)
        }

        return camelCase.toString()
    }

}