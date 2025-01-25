package com.frame.xplugins.flutter

import java.util.*
import com.frame.plugin.base.BaseAnActionForFlutter

class StateNotifierConsumerPage : BaseAnActionForFlutter("Controller State Page") {

    private val codePath = "/code/flutter/state_notifier_consumer"

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        //1、Page
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_page.dart",
            content = readFile("$codePath/simple_page.dart")!!
                .replace("^Simple^", className)
                .replace("^simple^", className.lowercase(Locale.getDefault())),
        )
        //2、State
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_state.dart",
            content = readFile("$codePath/simple_state.dart")!!
                .replace("^Simple^", className)
                .replace("^simple^", className.lowercase(Locale.getDefault())),
        )
        //3、Controller
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_controller.dart",
            content = readFile("$codePath/simple_controller.dart")!!
                .replace("^Simple^", className)
                .replace("^simple^", className.lowercase(Locale.getDefault())),
        )

    }


}