package com.frame.xplugins.getx.service

import com.frame.xplugins.getx.model.GetXScanResult
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.scanner.GetXProjectScanner
import com.frame.xplugins.getx.validation.GetXRouteValidator
import com.frame.xplugins.getx.writer.GetXRouteWriter
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project

class GetXRouteExecutor(private val project: Project) {

    fun execute(config: RouteGenerateConfig) {
        val freshScan = ReadAction.compute<GetXScanResult, RuntimeException> {
            GetXProjectScanner(project).scan(config.libDirectory, config.parentDirectory)
        }
        execute(config, freshScan)
    }

    fun execute(config: RouteGenerateConfig, freshScan: GetXScanResult) {
        val writer = GetXRouteWriter(project)
        ReadAction.run<RuntimeException> {
            GetXRouteValidator(project).validate(config, freshScan)
            writer.validateAppend(config)
        }
        WriteCommandAction.runWriteCommandAction(
            project,
            "Create GetX Route",
            null,
            Runnable {
                writer.write(config)
            },
        )
    }
}
