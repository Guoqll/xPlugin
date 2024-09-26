package com.frame.xplugins.view_binding

import java.util.*
import com.frame.plugin.base.BaseAnAction

class ViewBindingFragment : BaseAnAction("ViewBinding Fragment") {

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
        writeToFile(
            filepath = filePath,
            filename = "${className}Fragment.kt",
            content = readFile("$codePath/ViewBindingFragment.txt")!!.replace("^file_path^", packageName)
                .replace("^class_name^", className).replace(
                    "^FragmentHomeBinding^", "${resLimitStr.uppercase(Locale.getDefault())}Fragment${className}Binding"
                ),
        )

        writeToFile(
            filepath = filePath,
            filename = "${className}ViewModel.kt",
            content = readFile("$codePath/ViewBindingVM.txt")!!.replace("^file_path^", packageName)
                .replace("^class_name^", className),
        )

        val fragmentLayoutCodeFile: String = "fragment_layout.xml"
        writeToFile(
            filepath = "${mainPath}res/layout",
            filename = "${resLimitStr_}fragment_${className.lowercase(Locale.getDefault())}.xml",
            content = readFile("$codePath/$fragmentLayoutCodeFile")!!.replace("^file_path^", packageName)
                .replace("^class_name^", className),
        )
    }


}