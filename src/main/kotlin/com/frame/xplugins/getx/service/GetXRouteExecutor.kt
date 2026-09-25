package com.frame.xplugins.getx.service

import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.scanner.GetXProjectScanner
import com.frame.xplugins.getx.validation.GetXRouteValidator
import com.frame.xplugins.getx.writer.GetXRouteWriter
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project

class GetXRouteExecutor(private val project: Project) {

    fun execute(config: RouteGenerateConfig) {
        WriteCommandAction.runWriteCommandAction(
            project,
            "Create GetX Route",
            null,
            Runnable {
                val freshScan = GetXProjectScanner(project).scan(config.libDirectory)
                GetXRouteValidator().validate(config, freshScan)
                val writer = GetXRouteWriter(project)
                writer.validateAppend(config)
                writer.write(config)
            },
        )
    }
}
