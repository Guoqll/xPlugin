package com.frame.xplugins.getx.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.SmartPsiElementPointer
import com.jetbrains.lang.dart.psi.DartClass
import com.jetbrains.lang.dart.psi.DartMixinDeclaration

enum class PageType(val displayName: String) {
    GET_VIEW("GetView"),
    STATELESS("StatelessWidget"),
    STATEFUL("StatefulWidget"),
}

enum class PageCapability(val displayName: String) {
    BASIC("Basic"),
    DATA_STATE("Data State"),
}

enum class RouteDiMode(val displayName: String) {
    NAVIGATOR_PAGE_SCOPE("Navigator + Page Scope"),
    GETX_ROUTE_BINDING("GetX Route + Binding"),
}

enum class ControllerMode { CREATE, EXISTING, NONE }
enum class RepositoryMode { CREATE, EXISTING, NONE }
enum class BindingMode { CREATE, APPEND, NONE }

data class DartClassCandidate(
    val className: String,
    val file: VirtualFile,
    val relativePath: String,
    val pointer: SmartPsiElementPointer<DartClass>,
    val unnamedConstructorParameters: List<DartConstructorParameter>?,
) {
    override fun toString(): String = "$className  —  $relativePath"
}

data class DartConstructorParameter(
    val typeText: String?,
    val sourceText: String,
    val isOptional: Boolean,
)

data class BindingFileCandidate(
    val file: VirtualFile,
    val relativePath: String,
    val bindingClassNames: Set<String>,
) {
    override fun toString(): String = relativePath
}

data class PageControllerMixinCandidate(
    val file: VirtualFile,
    val relativePath: String,
    val pointer: SmartPsiElementPointer<DartMixinDeclaration>,
    val compatibilityErrors: List<String>,
) {
    val isCompatible: Boolean get() = compatibilityErrors.isEmpty()
}

data class NavigationEvidence(
    val hasMaterialApp: Boolean = false,
    val hasNavigatorRoutes: Boolean = false,
    val hasGetMaterialApp: Boolean = false,
    val hasGetXRoutes: Boolean = false,
) {
    val recommendedMode: RouteDiMode?
        get() = when {
            (hasMaterialApp || hasNavigatorRoutes) && !hasGetXRouteInfrastructure ->
                RouteDiMode.NAVIGATOR_PAGE_SCOPE
            hasGetXRouteInfrastructure && !hasNavigatorRoutes -> RouteDiMode.GETX_ROUTE_BINDING
            else -> null
        }

    val hasGetXRouteInfrastructure: Boolean
        get() = hasGetMaterialApp || hasGetXRoutes
}

data class GetXScanResult(
    val controllers: List<DartClassCandidate> = emptyList(),
    val repositories: List<DartClassCandidate> = emptyList(),
    val bindings: List<BindingFileCandidate> = emptyList(),
    val pageControllerMixin: PageControllerMixinCandidate? = null,
    val navigationEvidence: NavigationEvidence = NavigationEvidence(),
)

data class RouteGenerateConfig(
    val parentDirectory: VirtualFile,
    val libDirectory: VirtualFile,
    val routeName: String,
    val routeClassName: String,
    val prefix: String,
    val businessDomain: String,
    val pageType: PageType,
    val controllerMode: ControllerMode,
    val existingController: DartClassCandidate?,
    val repositoryMode: RepositoryMode,
    val existingRepository: DartClassCandidate?,
    val bindingMode: BindingMode,
    val existingBinding: BindingFileCandidate?,
    val createWidgetsDirectory: Boolean,
    val pageCapability: PageCapability = PageCapability.BASIC,
    val enablePullToRefresh: Boolean = true,
    val generateMockExample: Boolean = true,
    val routeDiMode: RouteDiMode = RouteDiMode.GETX_ROUTE_BINDING,
) {
    val targetPath: String = "${parentDirectory.path}/$routeName"
    val fileStem: String = listOf(prefix, routeName).filter { it.isNotEmpty() }.joinToString("_")
    val domainClassName: String = businessDomain.split('_').joinToString("") {
        it.replaceFirstChar(Char::uppercaseChar)
    }
    val pageClassName: String = "${routeClassName}Page"
    val controllerClassName: String = existingController?.className ?: "${routeClassName}Controller"
    val repositoryClassName: String? = when (repositoryMode) {
        RepositoryMode.CREATE -> "${domainClassName}Repository"
        RepositoryMode.EXISTING -> existingRepository?.className
        RepositoryMode.NONE -> null
    }
    val bindingClassName: String = "${routeClassName}Binding"
    val pageFileName: String = "${fileStem}_page.dart"
    val controllerFileName: String = "${fileStem}_controller.dart"
    val repositoryFileName: String = "${listOf(prefix, businessDomain).filter { it.isNotEmpty() }.joinToString("_")}_repository.dart"
    val bindingFileName: String = "${listOf(prefix, businessDomain).filter { it.isNotEmpty() }.joinToString("_")}_binding.dart"
    val pageStateMixinPath: String = "${libDirectory.path}/core/mixins/page_state_mixin.dart"
    val pageStateWidgetsPath: String = "${libDirectory.path}/core/widgets/page_state/page_state_widgets.dart"
    val pageControllerMixinPath: String = "${libDirectory.path}/core/mixins/page_controller_mixin.dart"
    val repositoryRegistrationMethodName: String? = repositoryClassName?.let { "_register$it" }
}

data class RouteGenerationPlan(
    val createPaths: List<String>,
    val reuseEntries: List<String>,
    val modifyEntries: List<String>,
    val createCommonInfrastructurePaths: List<String> = emptyList(),
    val reuseCommonInfrastructurePaths: List<String> = emptyList(),
    val showCommonInfrastructure: Boolean = false,
    val routeDiMode: RouteDiMode = RouteDiMode.GETX_ROUTE_BINDING,
) {
    fun render(): String = buildString {
        appendLine("ROUTE / DI MODE")
        appendLine("  ${routeDiMode.displayName}")
        appendLine()
        appendLine("CREATE")
        if (createPaths.isEmpty()) appendLine("  (none)") else createPaths.forEach { appendLine("  $it") }
        if (showCommonInfrastructure) {
            appendLine()
            appendLine("CREATE COMMON INFRASTRUCTURE")
            if (createCommonInfrastructurePaths.isEmpty()) {
                appendLine("  (none)")
            } else {
                createCommonInfrastructurePaths.forEach { appendLine("  $it") }
            }
            appendLine()
            appendLine("REUSE COMMON INFRASTRUCTURE")
            if (reuseCommonInfrastructurePaths.isEmpty()) {
                appendLine("  (none)")
            } else {
                reuseCommonInfrastructurePaths.forEach { appendLine("  $it") }
            }
        }
        appendLine()
        appendLine("REUSE")
        if (reuseEntries.isEmpty()) appendLine("  (none)") else reuseEntries.forEach { appendLine("  $it") }
        appendLine()
        appendLine("MODIFY")
        if (modifyEntries.isEmpty()) appendLine("  (none)") else modifyEntries.forEach { appendLine("  $it") }
    }.trimEnd()
}

class GetXGenerationException(message: String) : RuntimeException(message)
