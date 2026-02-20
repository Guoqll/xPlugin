package com.frame.xplugins.flutter

import com.frame.plugin.base.BaseAnActionForFlutter

class ConsumerWidgetMvvmPage : BaseAnActionForFlutter("stless@riverpod") {

    private val codePath = "/code/flutter/riverpod2"

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

    // 扩展函数：将字符串的首字母转为小写
    fun String.toLowerCaseFirstChar(): String {
        if (this.isEmpty()) return this
        return this[0].lowercaseChar() + this.substring(1)
    }
// 示例: NewMain → new_main

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        //1、Page
        writeToFile(
            filepath = filePath,
            filename = "${className.toSnakeCase()}_page.dart",
            content = readFile("$codePath/example_stless_page.dart")!!
                .replace("Example", className)
                .replace("example", className.toLowerCaseFirstChar())
                .replace("${className.toLowerCaseFirstChar()}_state.dart", "${className.toSnakeCase()}_state.dart")
                .replace("${className.toLowerCaseFirstChar()}_view_model.dart", "${className.toSnakeCase()}_view_model.dart"),
        )
        //2、viewModel
        writeToFile(
            filepath = filePath,
            filename = "${className.toSnakeCase()}_view_model.dart",
            content = readFile("$codePath/example_view_model.dart")!!
                .replace("Example", className)
//                .replace("example", className.toLowerCaseFirstChar())
                .replace("example_state.dart", "${className.toSnakeCase()}_state.dart")
                .replace("example_view_model.dart", "${className.toSnakeCase()}_view_model.dart")
                .replace("example_view_model.g.dart", "${className.toSnakeCase()}_view_model.g.dart"),
        )
        //3、State
        writeToFile(
            filepath = filePath,
            filename = "${className.toSnakeCase()}_state.dart",
            content = readFile("$codePath/example_state.dart")!!
                .replace("Example", className)
                .replace("example_state.g.dart", "${className.toSnakeCase()}_state.g.dart")
                .replace("example_state.freezed.dart", "${className.toSnakeCase()}_state.freezed.dart"),
        )

    }


}