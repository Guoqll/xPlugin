package com.frame.xplugins.view_binding

import java.util.*
import com.frame.plugin.base.BaseAnAction

class ViewBindingActivity : BaseAnAction("ViewBinding Activity") {

    private val codePath = "/code/view_binding"

    override fun generationXcode(
        filePath: String, mainPath: String, packageName: String, className: String, resLimit: String
    ) {
        var resLimitStr = ""
        var resLimitStr_ = ""
        if (resLimit.isNotEmpty()) {
            resLimitStr = resLimit.lowercase(Locale.getDefault())
            resLimitStr_ = "${resLimit.lowercase(Locale.getDefault())}_"
        }
        //activity
        writeToFile(
            filepath = filePath,
            filename = "${className}Activity.kt",
            content = readFile("$codePath/ViewBindingActivity.txt")!!
                .replace("^file_path^", packageName)
                .replace("^class_name^", className)
                .replace(
                    "^ActivityMainBinding^",
                    "${resLimitStr.uppercase(Locale.getDefault())}Activity${className}Binding"
                ),
        )
        //view_model
        writeToFile(
            filepath = filePath,
            filename = "${className}ViewModel.kt",
            content = readFile("$codePath/ViewBindingVM.txt")!!.replace("^file_path^", packageName)
                .replace("^class_name^", className),
        )
        //layout
        val fragmentLayoutCodeFile: String = "activity_layout.xml"
        writeToFile(
            filepath = "${mainPath}res/layout",
            filename = "${resLimitStr_}activity_${className.lowercase(Locale.getDefault())}.xml",
            content = readFile("$codePath/$fragmentLayoutCodeFile")!!.replace("^file_path^", packageName)
                .replace("^class_name^", className),
        )
    }

}