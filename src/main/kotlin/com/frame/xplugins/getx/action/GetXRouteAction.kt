package com.frame.xplugins.getx.action

import com.frame.xplugins.getx.dialog.GetXRouteDialog
import com.frame.xplugins.getx.model.GetXGenerationException
import com.frame.xplugins.getx.model.GetXScanResult
import com.frame.xplugins.getx.path.FlutterProjectPathResolver
import com.frame.xplugins.getx.scanner.GetXProjectScanner
import com.frame.xplugins.getx.service.GetXRouteExecutor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory

class GetXRouteAction : AnAction("GetX Route") {

    override fun update(e: AnActionEvent) {
        // Nested New-menu updates do not consistently expose directory data on
        // every supported IDE version. Validate the directory when invoked.
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selected = selectedDirectory(e) ?: return
        val pathContext = FlutterProjectPathResolver.resolve(selected) ?: run {
            Messages.showErrorDialog(project, "Select the lib directory or one of its subdirectories.", "GetX Route")
            return
        }
        val scan = scanDartClasses(
            project,
            pathContext.libDirectory,
            pathContext.parentDirectory,
            "Scanning required GetX files",
        ) ?: return
        val dialog = GetXRouteDialog(project, pathContext, scan)
        if (!dialog.showAndGet()) return

        val config = dialog.resultConfig()
        try {
            GetXRouteExecutor(project).execute(config, scan)
            Messages.showInfoMessage(project, "GetX route created successfully.", "GetX Route")
        } catch (error: GetXGenerationException) {
            Messages.showErrorDialog(project, error.message ?: "GetX route generation failed.", "GetX Route")
        } catch (error: Exception) {
            Messages.showErrorDialog(project, error.message ?: error.javaClass.simpleName, "GetX Route")
        }
    }

    private fun scanDartClasses(
        project: Project,
        libDirectory: VirtualFile,
        controllerSearchDirectory: VirtualFile,
        title: String,
    ): GetXScanResult? {
        var result: GetXScanResult? = null
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                result = ReadAction.compute<GetXScanResult, RuntimeException> {
                    GetXProjectScanner(project).scan(libDirectory, controllerSearchDirectory)
                }
            },
            title,
            true,
            project,
        )
        return result
    }

    private fun selectedDirectory(e: AnActionEvent): VirtualFile? {
        e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf(VirtualFile::isDirectory)?.let { return it }

        val element = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (element is PsiDirectory) return element.virtualFile

        e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.singleOrNull()
            ?.takeIf(VirtualFile::isDirectory)
            ?.let { return it }

        return e.getData(LangDataKeys.IDE_VIEW)
            ?.directories
            ?.singleOrNull()
            ?.virtualFile
    }
}
