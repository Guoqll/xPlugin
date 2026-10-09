package com.frame.xplugins.getx.validation

import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.DartClassCandidate
import com.frame.xplugins.getx.model.GetXGenerationException
import com.frame.xplugins.getx.model.GetXScanResult
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.scanner.GetXProjectScanner
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class GetXRouteValidator(private val project: Project) {

    fun validate(config: RouteGenerateConfig, scan: GetXScanResult) {
        val errors = mutableListOf<String>()
        if (!config.parentDirectory.isValid || !config.parentDirectory.isDirectory) {
            errors += "The selected directory is no longer available."
        }
        if (!config.libDirectory.isValid) errors += "The Flutter lib directory is no longer available."
        if (config.pageType == PageType.GET_VIEW && config.controllerMode == ControllerMode.NONE) {
            errors += "GetView requires a controller."
        }
        if (config.pageCapability == PageCapability.DATA_STATE) {
            val expectedPageType = if (config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE) {
                PageType.STATEFUL
            } else {
                PageType.GET_VIEW
            }
            if (config.pageType != expectedPageType) {
                errors += "Data State requires a ${expectedPageType.displayName} page in ${config.routeDiMode.displayName} mode."
            }
            if (config.controllerMode != ControllerMode.CREATE) errors += "Data State requires a new controller."
            validateCommonInfrastructurePath(
                config.libDirectory,
                listOf("core", "mixins", "page_state_mixin.dart"),
                errors,
            )
            validateCommonInfrastructurePath(
                config.libDirectory,
                listOf("core", "widgets", "page_state", "page_state_widgets.dart"),
                errors,
            )
        }
        if (config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE) {
            if (config.controllerMode != ControllerMode.NONE && config.pageType != PageType.STATEFUL) {
                errors += "Navigator Page Scope requires a StatefulWidget when a controller is used."
            }
            if (config.bindingMode != BindingMode.NONE) errors += "Navigator Page Scope does not use a Binding."
            if (config.controllerMode != ControllerMode.NONE) {
                validateCommonInfrastructurePath(
                    config.libDirectory,
                    listOf("core", "mixins", "page_controller_mixin.dart"),
                    errors,
                )
                validatePageControllerMixin(config, scan, errors)
            }
        }
        if (config.controllerMode == ControllerMode.NONE) {
            if (config.repositoryMode != RepositoryMode.NONE) errors += "A repository requires a controller."
            if (config.bindingMode != BindingMode.NONE) errors += "A binding requires a controller."
        }
        if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING &&
            config.controllerMode == ControllerMode.CREATE &&
            config.bindingMode == BindingMode.NONE
        ) {
            errors += "A newly created controller must be registered by a new or existing binding file."
        }
        if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING &&
            config.controllerMode == ControllerMode.EXISTING &&
            config.bindingMode == BindingMode.NONE &&
            config.repositoryMode != RepositoryMode.NONE
        ) {
            errors += "Repository selection has no effect when an existing controller uses no new binding."
        }
        if (config.controllerMode == ControllerMode.EXISTING && config.existingController == null) {
            errors += "Select an existing controller."
        }
        if (config.repositoryMode == RepositoryMode.EXISTING && config.existingRepository == null) {
            errors += "Select an existing repository."
        }
        if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING &&
            config.bindingMode == BindingMode.APPEND &&
            config.existingBinding == null
        ) {
            errors += "Select an existing binding file."
        }
        val assemblesController = config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE ||
            config.bindingMode != BindingMode.NONE

        val targetDirectory = config.parentDirectory.findChild(config.routeName)
        checkFileConflict(targetDirectory, config.pageFileName, errors)
        if (config.controllerMode == ControllerMode.CREATE) {
            checkFileConflict(targetDirectory, config.controllerFileName, errors)
        }
        if (config.repositoryMode == RepositoryMode.CREATE) {
            checkFileConflict(config.libDirectory.findChild("repositories"), config.repositoryFileName, errors)
        }
        if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING && config.bindingMode == BindingMode.CREATE) {
            checkFileConflict(config.libDirectory.findChild("bindings"), config.bindingFileName, errors)
        }
        val generatedClasses = buildList {
            add(config.pageClassName)
            if (config.controllerMode == ControllerMode.CREATE) add(config.controllerClassName)
            if (config.repositoryMode == RepositoryMode.CREATE) add(requireNotNull(config.repositoryClassName))
            if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING && config.bindingMode != BindingMode.NONE) {
                add(config.bindingClassName)
            }
            if (config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE &&
                config.controllerMode != ControllerMode.NONE &&
                config.libDirectory.fileSystem.findFileByPath(config.pageControllerMixinPath) == null
            ) {
                add("PageControllerMixin")
            }
        }
        val classScanner = GetXProjectScanner(project)
        generatedClasses.filter { className -> classScanner.containsClassName(className, config.libDirectory) }
            .forEach { errors += "Class $it already exists." }
        val refreshedController = config.existingController?.let(classScanner::refreshCandidate)
        val refreshedRepository = config.existingRepository?.let(classScanner::refreshCandidate)
        if (config.existingController != null && refreshedController == null) {
            errors += "${config.existingController.className} is no longer available. Rescan the project and try again."
        }
        if (config.existingRepository != null && refreshedRepository == null) {
            errors += "${config.existingRepository.className} is no longer available. Rescan the project and try again."
        }
        if (assemblesController && config.controllerMode == ControllerMode.EXISTING && refreshedController != null) {
            validateExistingControllerConstructor(config, refreshedController, errors)
        }
        if (assemblesController && config.repositoryMode == RepositoryMode.EXISTING && refreshedRepository != null) {
            validateExistingRepositoryConstructor(refreshedRepository, errors)
        }
        config.existingBinding?.let { binding ->
            if (!binding.file.isValid) errors += "${binding.relativePath} is no longer available. Rescan the project and try again."
        }
        if (config.existingBinding?.bindingClassNames?.contains(config.bindingClassName) == true) {
            errors += "Class ${config.bindingClassName} already exists in ${config.existingBinding.relativePath}."
        }
        if (errors.isNotEmpty()) throw GetXGenerationException(errors.distinct().joinToString("\n"))
    }

    private fun validateExistingControllerConstructor(
        config: RouteGenerateConfig,
        candidate: DartClassCandidate,
        errors: MutableList<String>,
    ) {
        val parameters = candidate.unnamedConstructorParameters
        if (parameters == null) {
            errors += "${candidate.className} has no unnamed constructor that the binding can call."
            return
        }
        val repositoryClass = config.repositoryClassName
        if (repositoryClass == null && parameters.isNotEmpty()) {
            errors += "${candidate.className} does not have a zero-argument unnamed constructor."
        }
        if (repositoryClass != null && (
                parameters.size != 1 ||
                    parameters.single().isOptional ||
                    parameters.single().typeText?.substringAfterLast('.') != repositoryClass
                )
        ) {
            errors += "${candidate.className} must have one unnamed constructor parameter of type $repositoryClass."
        }
    }

    private fun validateExistingRepositoryConstructor(
        candidate: DartClassCandidate,
        errors: MutableList<String>,
    ) {
        val parameters = candidate.unnamedConstructorParameters
        if (parameters == null) {
            errors += "${candidate.className} has no unnamed constructor that dependency assembly can call."
            return
        }
        if (parameters.isEmpty()) return
        if (parameters.size != 1 ||
            parameters.single().isOptional ||
            parameters.single().typeText?.substringAfterLast('.') != "TkApi"
        ) {
            errors += "${candidate.className} must have either a zero-argument unnamed constructor or one required TkApi parameter."
        }
    }

    private fun validatePageControllerMixin(
        config: RouteGenerateConfig,
        scan: GetXScanResult,
        errors: MutableList<String>,
    ) {
        val mixinFile = config.libDirectory.fileSystem.findFileByPath(config.pageControllerMixinPath) ?: return
        if (mixinFile.isDirectory) return
        val candidate = scan.pageControllerMixin?.let(GetXProjectScanner(project)::refreshPageControllerMixin)
        if (candidate == null || candidate.file != mixinFile) {
            errors += "page_controller_mixin.dart exists but PageControllerMixin cannot be safely identified."
        } else if (!candidate.isCompatible) {
            errors += candidate.compatibilityErrors.map { "PageControllerMixin: $it" }
        }
    }

    private fun checkFileConflict(directory: VirtualFile?, fileName: String, errors: MutableList<String>) {
        if (directory?.findChild(fileName) != null) errors += "$fileName already exists."
    }

    private fun validateCommonInfrastructurePath(
        libDirectory: VirtualFile,
        segments: List<String>,
        errors: MutableList<String>,
    ) {
        var current = libDirectory
        segments.forEachIndexed { index, segment ->
            val child = current.findChild(segment) ?: return
            val isTarget = index == segments.lastIndex
            if (isTarget && child.isDirectory) {
                errors += "${segments.joinToString("/")} must be a Dart file, but a directory already exists."
                return
            }
            if (!isTarget && !child.isDirectory) {
                errors += "Cannot create ${segments.joinToString("/")}: ${segments.take(index + 1).joinToString("/")} is a file."
                return
            }
            current = child
        }
    }
}
