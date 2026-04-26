package com.frame.xplugins.flutter

import com.frame.plugin.base.BaseAnActionForFlutter

class Consumer3StfulWidgetMvvmPage : BaseAnActionForFlutter("stful@riverpod3") {

    private val codePath = "/code/flutter/riverpod3"

    // 扩展函数：将驼峰式命名转换为蛇形命名（首字母小写，大写字母前加下划线）
    private fun String.toSnakeCase(): String {
        val regex = "([a-z])([A-Z]+)".toRegex()
        return this.replace(regex, "$1_$2").lowercase()
    }

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        val snakeClassName = className.toSnakeCase()
        // 使用 Kotlin 标准库的方法将首字母转小写
        val lowerCamelClassName = className.replaceFirstChar { it.lowercase() }

        // 统一提取模板替换逻辑，避免代码重复，并保证替换顺序的正确性
        fun String.replaceTemplatePlaceholders(): String {
            return this
                // 1. 优先替换所有的特定文件名后缀，防止后续被 "example" 的变量替换破坏
                .replace("demo_state.g.dart", "${snakeClassName}_state.g.dart")
                .replace("demo_state.freezed.dart", "${snakeClassName}_state.freezed.dart")
                .replace("demo_view_model.g.dart", "${snakeClassName}_view_model.g.dart")
                .replace("demo_state.dart", "${snakeClassName}_state.dart")
                .replace("demo_view_model.dart", "${snakeClassName}_view_model.dart")
                // 2. 替换类名
                .replace("Demo", className)
                // 3. 替换变量名
                .replace("demo", lowerCamelClassName)
        }

        // 读取模板内容的辅助函数，增加空安全检查，避免抛出 NPE
        fun readTemplate(fileName: String): String {
            val fullPath = "$codePath/$fileName"
            return requireNotNull(readFile(fullPath)) { 
                "Template file not found in plugin resources: $fullPath" 
            }
        }

        // 1、Page
        writeToFile(
            filepath = filePath,
            filename = "${snakeClassName}_page.dart",
            content = readTemplate("demo_page.dart").replaceTemplatePlaceholders()
        )

        // 2、viewModel
        writeToFile(
            filepath = filePath,
            filename = "${snakeClassName}_view_model.dart",
            content = readTemplate("demo_view_model.dart").replaceTemplatePlaceholders()
        )

        // 3、State
        writeToFile(
            filepath = filePath,
            filename = "${snakeClassName}_state.dart",
            content = readTemplate("demo_state.dart").replaceTemplatePlaceholders()
        )
    }
}
