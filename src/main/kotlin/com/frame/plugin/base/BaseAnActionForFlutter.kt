package com.frame.plugin.base

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.io.IOUtils
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.*
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

abstract class BaseAnActionForFlutter(actionText: String) : AnAction(actionText) {

    private val mTitle = "NewMvvmGroup"
    private val mDescriptor = "please input view name"
    private var resLimitation = "x"

    private lateinit var filePath: String
    private lateinit var mainPath: String
    private lateinit var packageName: String

    private val mDialogTitle = "dynamically generate template code"//"动态生成模版代码"
    private val mInputClassNameTitle = "please enter the class name (case sensitive):"//"请输入类名称(区分大小写):"
    private val mInputResLimitationTitle = "please enter a resource qualifier (which can be null):"//"请输入资源限定符(可为null):"


    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->

            getUserInputs(project).let { (className, resLimitation) ->
                if (className.isNullOrEmpty()) {
                    return
                }
                if (className.trim().isEmpty()) {
                    return
                }
                var resLimit: String = ""
                resLimitation?.let {
                    resLimit = it.trim()
                }
                CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)?.let { fileInfo ->
                    initBaseInfo(fileInfo, className)
                    generationXcode(filePath, mainPath, packageName, className, resLimit)
                }
            }
            project.baseDir.refresh(false, true)
        }
    }

    private fun getSingleInput(project: Project?): String? {
        val fileName = Messages.showInputDialog(project, mDescriptor, mTitle, Messages.getQuestionIcon())
        return fileName
    }

    private fun getUserInputs(project: Project?): Pair<String?, String?> {
        // 创建输入字段
        val firstInputField = JTextField(60)
        val secondInputField = JTextField(60)

        // 创建面板并使用 GridBagLayout
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // 设置约束以左对齐标签和输入字段
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0 // 输入框占用额外的空间

        // 添加第一个输入的标签
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.LINE_START // 标签左对齐
        panel.add(JLabel(mInputClassNameTitle), gbc)

        // 添加第一个输入框
        gbc.gridy = 1 // 移动到下一行
        panel.add(firstInputField, gbc)

        // 添加第二个输入的标签
        gbc.gridx = 0 // 切换回第一列
        gbc.gridy = 2 // 移动到下一行
        panel.add(JLabel(mInputResLimitationTitle), gbc)

        // 添加第二个输入框
        gbc.gridy = 3 // 移动到下一行
        panel.add(secondInputField, gbc)

        // 创建对话框
        val dialogBuilder = DialogBuilder()
        dialogBuilder.setCenterPanel(panel)
        dialogBuilder.setTitle(mDialogTitle)
        dialogBuilder.setOkOperation {
            dialogBuilder.dialogWrapper.close(0) // 关闭对话框
        }

        // 显示对话框
        dialogBuilder.show()

        // 返回用户输入的信息
        return Pair(firstInputField.text, secondInputField.text)
    }

    // 扩展函数：将驼峰式命名转换为蛇形命名（首字母小写，大写字母前加下划线）
    private fun String.toSnakeCase(): String {
        if (isEmpty()) return this
        val result = StringBuilder()
        // 首字母直接转小写
        result.append(this[0].lowercaseChar())
        for (i in 1 until length) {
            val currentChar = this[i]
            if (currentChar.isUpperCase()) {
                // 大写字母前加下划线并转小写
                result.append('_').append(currentChar.lowercaseChar())
            } else {
                result.append(currentChar)
            }
        }
        return result.toString()
    }


    private fun initBaseInfo(selectGroup: VirtualFile?, className: String) {
        // 将驼峰类名转换为蛇形路径（如 NewMain → new_main）
        val classPathName =
            className.toSnakeCase()//className.replace(Regex("(?<!^)([A-Z])"), "_$1").lowercase(Locale.getDefault())

        filePath = selectGroup!!.path + "/${className.toSnakeCase()}" //"/${className.lowercase(Locale.getDefault())}"
        packageName = filePath.substring(filePath.indexOf("lib") + 4, filePath.length).replace("/", ".")
        mainPath = filePath.substring(0, filePath.indexOf("lib"))
    }

    abstract fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    )

    protected fun readFile(filename: String): String? {
        var codeString = ""
        try {
            var inputStream: InputStream? = null
            inputStream = this.javaClass.getResourceAsStream(filename)
            val writer = StringWriter()
            IOUtils.copy(inputStream, writer, "UTF-8")
            codeString = writer.toString()
        } catch (e: Exception) {
            println("error:${e.message}")
        }
        return codeString
    }

    protected fun writeToFile(filepath: String, filename: String, content: String) {
        try {
            val floder = File(filepath)
            if (!floder.exists()) {
                floder.mkdirs()
            }
            val file = File("$filepath/$filename")
            if (!file.exists()) {
                file.createNewFile()
            }/*val fw = FileWriter(file.absoluteFile)
            val bw = BufferedWriter(fw)*/
            val bw = BufferedWriter(OutputStreamWriter(FileOutputStream(file.absoluteFile, true), "UTF-8"))
            bw.write(content)
            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}