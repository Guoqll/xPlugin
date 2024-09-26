package com.frame.plugin.flutter

import com.frame.plugin.PluginUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.*

class FlutterPage : AnAction("FlutterPage") {

    var project: Project? = null
    var selectGroup: VirtualFile? = null

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project
        val className = PluginUtil.getClassName(project)

        selectGroup = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
        if (className == null || className == "") {
            print("请输入类名")
            return
        }
        createMvpBase(className)
        project!!.baseDir.refresh(false, true)
    }

    /**
     * Flutter Mvi Page
     */
    private fun createMvpBase(className2: String) {
        val className = className2.replace("(.)([A-Z])".toRegex(), "$1_$2").lowercase(Locale.getDefault())
        val routePath = selectGroup!!.path
        val newClassPath = selectGroup!!.path + "/${className.toLowerCase()}"
        val packageName = newClassPath.substring(newClassPath.indexOf("lib") + 4, newClassPath.length).replace("/", ".")
        val rootPath = newClassPath.substring(0, newClassPath.indexOf("lib"))
        val generatedPath = "$rootPath/lib/generated"
        var classPageState = "${className2}PageState";
        val output2 = classPageState.replace("(.)([A-Z])".toRegex(), "$1_$2").lowercase(Locale.getDefault())

        //======FlutterPage
        var page = PluginUtil.readFile("BaseFlutterPage.txt")!!
        page = page.replace("&package&", packageName)
            .replace("&Home&", "$className2")
            .replace("&HomeViewModel&", "${className2}PageViewModel")
            .replace("&HomePageState&", "$output2")
            .replace("&home_page_view_model&", output2)

        var fileName = "${className}_page"
//        val classPageName = fileName.replace("(.)([A-Z])".toRegex(), "$1_$2").lowercase(Locale.getDefault())
        PluginUtil.writetoFile(page, newClassPath, "${fileName}.dart")

        //======FlutterPageState
        var viewmodel = PluginUtil.readFile("BaseFlutterPageViewModel.txt")!!
        viewmodel = viewmodel.replace("&Home&", "$className2")

        PluginUtil.writetoFile(viewmodel, newClassPath, "${output2}.dart")

        //try add routeMap
        var stringBuiffer = StringBuffer()
        stringBuiffer.append("import 'package:flutter/cupertino.dart';\n")

        val directory: File = File(routePath)
        if (directory.exists() && directory.isDirectory) {
            val subDirectories = directory.listFiles { file -> file.isDirectory() }
            val subDirectoryNames = subDirectories?.map { it.name }

            var importStr = StringBuilder()
            var constantStr = StringBuilder()
            var mapStr = StringBuilder()
            var allStr = StringBuilder()

            constantStr.append(
                "  static const rootPage = '/';\n"
            )

            //>>>>>>>>>>>>>import
            subDirectoryNames?.forEachIndexed { _, name ->
                importStr.append("import '../pages/$name/${name}_page.dart';\n")

                var constantStrBuilder = StringBuilder()
                if (name.contains("_")) {
                    name.split("_").forEachIndexed { _, str ->
                        if (str.isNotEmpty())
                            constantStrBuilder.append(str.capitalize())
                    }
                } else {
                    constantStrBuilder.append(name.capitalize())
                }
                constantStrBuilder.append("Page")
                constantStr.append(
                    "  static const ${constantStrBuilder.toString().decapitalize()} " +
                            "= '/pages/${constantStrBuilder.toString()}';\n"
                )
                mapStr.append(
                    "    ${
                        constantStrBuilder.toString().decapitalize()
                    }: (context) => const ${constantStrBuilder.toString()}(title: 'Hello, ${constantStrBuilder.toString()}.'),\n"
                )
            }
            allStr.append(
                "/*\n" +
                        " * #                                                   #\n" +
                        " * #                       _oo0oo_                     #\n" +
                        " * #                      o8888888o                    #\n" +
                        " * #                      88\" . \"88                    #\n" +
                        " * #                      (| -_- |)                    #\n" +
                        " * #                      0\\  =  /0                    #\n" +
                        " * #                    ___/`---'\\___                  #\n" +
                        " * #                  .' \\\\|     |# '.                 #\n" +
                        " * #                 / \\\\|||  :  |||# \\                #\n" +
                        " * #                / _||||| -:- |||||- \\              #\n" +
                        " * #               |   | \\\\\\  -  #/ |   |              #\n" +
                        " * #               | \\_|  ''\\---/''  |_/ |             #\n" +
                        " * #               \\  .-\\__  '-'  ___/-. /             #\n" +
                        " * #             ___'. .'  /--.--\\  `. .'___           #\n" +
                        " * #          .\"\" '<  `.___\\_<|>_/___.' >' \"\".         #\n" +
                        " * #         | | :  `- \\`.;`\\ _ /`;.`/ - ` : | |       #\n" +
                        " * #         \\  \\ `_.   \\_ __\\ /__ _/   .-` /  /       #\n" +
                        " * #     =====`-.____`.___ \\_____/___.-`___.-'=====    #\n" +
                        " * #                       `=---='                     #\n" +
                        " * #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #\n" +
                        " * #                                                   #\n" +
                        " * #               佛祖保佑         永无BUG              #\n" +
                        " * #                                                   #\n" +
                        " */\n\n"
            )
            allStr.append("import 'package:flutter/cupertino.dart';\n")
            allStr.append(importStr.toString())
            allStr.append("\n\n")
            allStr.append("//β One click generation of routing configuration\n")
            allStr.append("class RouteConfiguration {\n")
            allStr.append(constantStr.toString())
            allStr.append("  static final Map<String, WidgetBuilder> routeMap = <String, WidgetBuilder>{\n")
            allStr.append(mapStr.toString())
            allStr.append("  };\n}")

            val file = File("$generatedPath/route_configuration.dart")
            if (file.exists()) {
                file.delete()
            }
            PluginUtil.writetoFile(allStr.toString(), generatedPath, "route_configuration.dart")
        } else {
            println("The specified directory does not exist or is not a directory.")
        }
    }

}