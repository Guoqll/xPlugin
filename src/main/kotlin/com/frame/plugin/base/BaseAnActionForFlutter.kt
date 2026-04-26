package com.frame.plugin.base

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.apache.commons.io.IOUtils
import java.io.*
import javax.swing.JComponent
import javax.swing.JPanel

abstract class BaseAnActionForFlutter(actionText: String) : AnAction(actionText) {

    private val mDialogTitle = "Generate Flutter MVVM Files"
    private val mInputClassNameTitle = "Enter class name (e.g., UserProfile):"

    private lateinit var filePath: String
    private lateinit var mainPath: String
    private lateinit var packageName: String

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext) ?: return

        // 弹出输入框并获取用户输入的类名
        val className = getUserInput(project)
        if (className.isNullOrBlank()) {
            return
        }

        initBaseInfo(virtualFile, className)
        generationXcode(filePath, mainPath, packageName, className, "") // resLimit is no longer used

        project.baseDir.refresh(false, true)
    }

    /**
     * 使用 IntelliJ UI 组件创建一个更美观的输入对话框
     */
    private fun getUserInput(project: Project?): String? {
        val inputField = JBTextField()
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(mInputClassNameTitle, inputField)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        val dialogBuilder = DialogBuilder(project)
        dialogBuilder.setCenterPanel(panel)
        dialogBuilder.setTitle(mDialogTitle)
        dialogBuilder.setOkOperation {
            dialogBuilder.dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
        }

        // 将焦点设置到输入框
        dialogBuilder.setPreferredFocusComponent(inputField)

        val isOk = dialogBuilder.showAndGet()
        return if (isOk) inputField.text else null
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


    private fun initBaseInfo(selectGroup: VirtualFile, className: String) {
        // 将驼峰类名转换为蛇形路径（如 NewMain → new_main）
        val classPathName = className.toSnakeCase()

        filePath = selectGroup.path + "/$classPathName"
        packageName = filePath.substring(filePath.indexOf("lib") + 4).replace("/", ".")
        mainPath = filePath.substring(0, filePath.indexOf("lib"))
    }

    abstract fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    )

    protected fun readFile(filename: String): String? {
        return try {
            this.javaClass.getResourceAsStream(filename)?.use { inputStream ->
                StringWriter().use { writer ->
                    IOUtils.copy(inputStream, writer, "UTF-8")
                    writer.toString()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    protected fun writeToFile(filepath: String, filename: String, content: String) {
        try {
            val floder = File(filepath)
            if (!floder.exists()) {
                floder.mkdirs()
            }
            val file = File("$filepath/$filename")
            // 使用 try-with-resources 语法自动关闭流
            BufferedWriter(OutputStreamWriter(FileOutputStream(file, false), "UTF-8")).use {
                it.write(content)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
