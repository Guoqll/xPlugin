package com.frame.xplugins.flutter

import java.util.*
import com.frame.plugin.base.BaseAnActionForFlutter

class ChangeNotifierPage : BaseAnActionForFlutter("ChangeNotifier Page") {

    private val codePath = "/code/flutter/change_notifier"

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        /*var resLimitStr = ""
        var resLimitStr_ = ""
        if (resLimit.isNotEmpty()) {
            resLimitStr = resLimit.lowercase(Locale.getDefault())
            resLimitStr_ = "${resLimit.lowercase(Locale.getDefault())}_"
        }*/
        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_page.dart",
            content = readFile("$codePath/simple_page.dart")!!
                .replace("^Simple^", className),
        )

        writeToFile(
            filepath = filePath,
            filename = "${className.lowercase(Locale.getDefault())}_state.dart",
            content = readFile("$codePath/simple_notifier.dart")!!
                .replace("^Simple^", className),
        )

    }


}