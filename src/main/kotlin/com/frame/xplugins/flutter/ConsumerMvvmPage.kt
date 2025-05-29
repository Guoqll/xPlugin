package com.frame.xplugins.flutter

import java.util.*
import com.frame.plugin.base.BaseAnActionForFlutter

class ConsumerMvvmPage : BaseAnActionForFlutter("Create Mvvm Code") {

    private val codePath = "/code/flutter/mvvm"

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
            content = readFile("$codePath/home_page.dart")!!.replace("Home", className)
//                .replace("home", className.lowercase(Locale.getDefault()))
                .replace("home", className.toLowerCaseFirstChar())
                .replace("ho_me", className.toSnakeCase()),
        )
        //2、Mixin
        writeToFile(
            filepath = filePath,
            filename = "${className.toSnakeCase()}_mixin.dart",
            content = readFile("$codePath/home_mixin.dart")!!.replace("Home", className)
                .replace("home", className.toLowerCaseFirstChar())
                .replace("ho_me", className.toSnakeCase()),
        )
        //3、State
        writeToFile(
            filepath = filePath,
            filename = "${className.toSnakeCase()}_state.dart",
            content = readFile("$codePath/home_state.dart")!!.replace("Home", className)
                .replace("home", className.toLowerCaseFirstChar())
                .replace("ho_me", className.toSnakeCase()),
        )
        //4、Repository
        writeToFile(
            filepath = filePath,
            filename = "${className.toSnakeCase()}_repository.dart",
            content = readFile("$codePath/home_repository.dart")!!.replace("Home", className)
                .replace("home", className.toLowerCaseFirstChar())
                .replace("ho_me", className.toSnakeCase()),
        )

    }


}