package com.frame.xplugins.getx.service

import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.model.RouteGenerationPlan

class GetXRouteGenerationService {

    fun plan(config: RouteGenerateConfig): RouteGenerationPlan {
        val create = buildList {
            add(displayPath(config, "${config.targetPath}/${config.pageFileName}"))
            if (config.controllerMode == ControllerMode.CREATE) {
                add(displayPath(config, "${config.targetPath}/${config.controllerFileName}"))
            }
            if (config.createWidgetsDirectory) add(displayPath(config, "${config.targetPath}/widgets/"))
            if (config.repositoryMode == RepositoryMode.CREATE) {
                add(displayPath(config, "${config.libDirectory.path}/repositories/${config.repositoryFileName}"))
            }
            if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING && config.bindingMode == BindingMode.CREATE) {
                add(displayPath(config, "${config.libDirectory.path}/bindings/${config.bindingFileName}"))
            }
        }
        val reuse = buildList {
            config.existingController?.takeIf { config.controllerMode == ControllerMode.EXISTING }?.let {
                add("${it.className}\n    ${it.relativePath}")
            }
            config.existingRepository?.takeIf { config.repositoryMode == RepositoryMode.EXISTING }?.let {
                add("${it.className}\n    ${it.relativePath}")
            }
        }
        val modify = buildList {
            config.existingBinding?.takeIf {
                config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING && config.bindingMode == BindingMode.APPEND
            }?.let {
                add("${it.relativePath}\n    + required imports\n    + ${config.bindingClassName}")
            }
        }
        val createCommon = mutableListOf<String>()
        val reuseCommon = mutableListOf<String>()
        val commonPaths = mutableListOf<String>()
        if (config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE &&
            config.controllerMode != ControllerMode.NONE
        ) {
            commonPaths += config.pageControllerMixinPath
        }
        if (config.pageCapability == PageCapability.DATA_STATE) {
            commonPaths += listOf(config.pageStateMixinPath, config.pageStateWidgetsPath)
        }
        commonPaths.distinct().forEach { path ->
            val entries = if (config.libDirectory.fileSystem.findFileByPath(path) == null) createCommon else reuseCommon
            entries += displayPath(config, path)
        }
        return RouteGenerationPlan(
            createPaths = create,
            reuseEntries = reuse,
            modifyEntries = modify,
            createCommonInfrastructurePaths = createCommon,
            reuseCommonInfrastructurePaths = reuseCommon,
            showCommonInfrastructure = commonPaths.isNotEmpty(),
            routeDiMode = config.routeDiMode,
        )
    }

    private fun displayPath(config: RouteGenerateConfig, absolutePath: String): String {
        val moduleRoot = config.libDirectory.parent?.path ?: return absolutePath
        return absolutePath.removePrefix("$moduleRoot/")
    }
}
