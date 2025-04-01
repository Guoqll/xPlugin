package com.frame.xplugins.flutter

import java.util.*
import com.frame.plugin.base.BaseAnActionForFlutter

class ConsumerMvvmPage : BaseAnActionForFlutter("Create Mvvm Code") {

    private val codePath = "/code/flutter/mvvm"

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        //1、Page
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_page.dart",
            content = readFile("$codePath/home_page.dart")!!
                .replace("Home", className)
                .replace("home", className.lowercase(Locale.getDefault())),
        )
        //2、State
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_state.dart",
            content = readFile("$codePath/home_state.dart")!!
                .replace("Home", className)
                .replace("home", className.lowercase(Locale.getDefault())),
        )
        //3、Controller
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_repository.dart",
            content = readFile("$codePath/home_repository.dart")!!
                .replace("Home", className)
                .replace("home", className.lowercase(Locale.getDefault())),
        )

    }


}