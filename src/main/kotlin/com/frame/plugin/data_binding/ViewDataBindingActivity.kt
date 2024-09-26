package com.frame.plugin.data_binding

import com.frame.plugin.PluginUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class ViewDataBindingActivity : AnAction("ViewDataBindingActivity") {

    var project: Project? = null
    var selectGroup: VirtualFile? = null

    override fun actionPerformed(e: AnActionEvent) {
        project = e.project
        val className = PluginUtil.getClassName(project)

        selectGroup = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
        if (className == null || className == "") {
            print("没有输入类名")
            return
        }
        createMvpBase(className)
        project!!.baseDir.refresh(false, true)
    }

    /**
     * 创建MVP的Base文件夹
     */
    private fun createMvpBase(className: String) {
        val path = selectGroup!!.path + "/${className.toLowerCase()}"
        val packageName = path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
        val main = path.substring(0, path.indexOf("java"))

        //Activity
        var activity = PluginUtil.readFile("BaseViewDataBindingActivity.txt")!!
        activity = PluginUtil.replaceActivityCode(activity, packageName, className)
        /*.replace("&package&", packageName)
        .replace("&SimpleActivity&", "${className}Activity")
        .replace("&ActivitySplashBinding&", "${PluginUtil.getResLimitation().toUpperCase()}Activity${className}Binding")
        .replace("&ActivityViewBinding&", "${PluginUtil.getResLimitation().toUpperCase()}Activity${className}Binding")
        .replace("&SimpleViewModel&", "${className}ViewModel")
        .replace("&R.layout.activity_simple&", "R.layout.${PluginUtil.getResLimitation()}_activity_${className.toLowerCase()}")
        .replace("&SimpleViewModel&", "${className}ViewModel")*/
        PluginUtil.writetoFile(activity, path, "${className}Activity.kt")

        //ViewModel
        var viewmodel = PluginUtil.readFile("BaseViewModel.txt")!!
        viewmodel = PluginUtil.replaceViewModel(viewmodel, packageName, className)
        PluginUtil.writetoFile(viewmodel, path, "${className}ViewModel.kt")

        //R.layout.xxx.xml
        val layoutXML = PluginUtil.readFile("BaseViewDataBindingLayout.txt")!!
            .replace("&.SplashActivity&", ".${className.toLowerCase()}.${className}Activity")
            .replace(
                "&com.load.md_simeple.SimpleViewModel&",
                "$packageName.${className}ViewModel"
            )
        PluginUtil.writetoFile(layoutXML, "${main}res/layout", "${PluginUtil.getResLimitation()}_activity_${className.toLowerCase()}.xml")

        //======BaseIntent
        var xIntent = PluginUtil.readFile("BaseIntent.txt")!!
        xIntent = PluginUtil.replaceIntent(xIntent, packageName, className)
        PluginUtil.writetoFile(xIntent, path, "${className}Intent.kt")

    }


}