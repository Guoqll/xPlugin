package com.frame.xplugins.flutter

import java.util.*
import com.frame.plugin.base.BaseAnActionForFlutter

class StateNotifierConsumerPage : BaseAnActionForFlutter("ConsumerPage") {

    private val codePath = "/code/flutter/state_notifier_consumer"

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_page.dart",
            content = readFile("$codePath/simple_page.dart")!!
                .replace("^Simple^", className)
                .replace("^simple^", className.lowercase(Locale.getDefault())),
        )

        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_state.dart",
            content = readFile("$codePath/simple_provider.dart")!!
                .replace("^Simple^", className)
                .replace("^simple^", className.lowercase(Locale.getDefault())),
        )

    }


}