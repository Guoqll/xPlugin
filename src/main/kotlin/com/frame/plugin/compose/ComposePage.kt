package com.frame.plugin.compose

import com.frame.plugin.PluginUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class ComposePage : AnAction("ComposePage") {

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
     * Compose Mvi Page
     */
    private fun createMvpBase(className: String) {
        val path = selectGroup!!.path + "/${className.toLowerCase()}"
        val packageName = path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
        val main = path.substring(0, path.indexOf("java"))

        PluginUtil.readAndroidManfiset(main)

        //======ComposePage
        var page = PluginUtil.readFile("BaseComposePage.txt")!!
        page = page.replace("&package&", packageName)
            .replace("&HomePage&", "${className}Page")
            .replace("&HomeViewModel&", "${className}PageViewModel")
            .replace("&Home&Intent", "${className}PageIntent")
        PluginUtil.writetoFile(page, path, "${className}Page.kt")

        //======ComposePageViewModel
        var viewmodel = PluginUtil.readFile("BaseComposePageViewModel.txt")!!
        viewmodel = viewmodel.replace("&package&", packageName)
            .replace("&Home&", "$className"+"Page")

        PluginUtil.writetoFile(viewmodel, path, "${className}PageViewModel.kt")


    }


}