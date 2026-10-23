package com.frame.xplugins.getx.writer

import com.frame.xplugins.getx.generator.GetXCodeGenerator
import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.GetXGenerationException
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.DartClass
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBodyOrNative
import com.jetbrains.lang.dart.psi.DartImportStatement
import com.jetbrains.lang.dart.psi.DartLibraryStatement

class GetXRouteWriter(
    private val project: Project,
    private val generator: GetXCodeGenerator = GetXCodeGenerator(),
) {
    private val psiManager = PsiManager.getInstance(project)
    private val documents = PsiDocumentManager.getInstance(project)

    fun validateAppend(config: RouteGenerateConfig) {
        if (config.routeDiMode != RouteDiMode.GETX_ROUTE_BINDING || config.bindingMode != BindingMode.APPEND) return
        val dartFile = psiManager.findFile(requireNotNull(config.existingBinding).file) as? DartFile
            ?: throw GetXGenerationException("The selected binding file is no longer a Dart file.")
        val classes = PsiTreeUtil.findChildrenOfType(dartFile, DartClass::class.java)
        if (classes.any { it.name == config.bindingClassName }) {
            throw GetXGenerationException("Class ${config.bindingClassName} already exists.")
        }
        if (config.repositoryClassName != null) {
            resolveRepositoryHelper(dartFile, config)
        }
    }

    fun write(config: RouteGenerateConfig) {
        val libDirectory = requireNotNull(psiManager.findDirectory(config.libDirectory))
        val parentDirectory = requireNotNull(psiManager.findDirectory(config.parentDirectory))
        val targetDirectory = ensureDirectory(parentDirectory, config.routeName)
        val createdFiles = mutableListOf<PsiFile>()

        createCommonInfrastructure(libDirectory, config, createdFiles)
        createdFiles += createDartFile(targetDirectory, config.pageFileName, generator.page(config))
        if (config.controllerMode == ControllerMode.CREATE) {
            createdFiles += createDartFile(targetDirectory, config.controllerFileName, generator.controller(config))
        }
        config.businessMixinFileName?.let { fileName ->
            createdFiles += createDartFile(targetDirectory, fileName, generator.businessMixin(config))
        }
        if (config.createWidgetsDirectory && targetDirectory.findSubdirectory("widgets") == null) {
            targetDirectory.createSubdirectory("widgets")
        }
        if (config.repositoryMode == RepositoryMode.CREATE) {
            val repositories = ensureDirectory(libDirectory, "repositories")
            createdFiles += createDartFile(repositories, config.repositoryFileName, generator.repository(config))
        }
        if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING && config.bindingMode == BindingMode.CREATE) {
            val bindings = ensureDirectory(libDirectory, "bindings")
            createdFiles += createDartFile(bindings, config.bindingFileName, generator.bindingFile(config))
        } else if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING && config.bindingMode == BindingMode.APPEND) {
            appendBinding(config)
        }
        createdFiles.forEach { file ->
            CodeStyleManager.getInstance(project).reformat(file)
        }
    }

    private fun createDartFile(directory: PsiDirectory, fileName: String, content: String): PsiFile {
        if (directory.findFile(fileName) != null) throw GetXGenerationException("$fileName already exists.")
        val file = PsiFileFactory.getInstance(project).createFileFromText(fileName, DartFileType.INSTANCE, content)
        return directory.add(file) as PsiFile
    }

    private fun ensureDirectory(parent: PsiDirectory, name: String): PsiDirectory =
        parent.findSubdirectory(name) ?: parent.createSubdirectory(name)

    private fun appendBinding(config: RouteGenerateConfig) {
        val dartFile = psiManager.findFile(requireNotNull(config.existingBinding).file) as? DartFile
            ?: throw GetXGenerationException("The selected binding file is no longer a Dart file.")
        val document = documents.getDocument(dartFile)
            ?: throw GetXGenerationException("Cannot open ${dartFile.name} for editing.")
        val existingImports = PsiTreeUtil.findChildrenOfType(dartFile, DartImportStatement::class.java)
            .mapNotNull { it.uriString }
            .toSet()
        val missingImports = generator.bindingImports(config).filterNot(existingImports::contains)
        val repositoryHelper = config.repositoryClassName?.let { resolveRepositoryHelper(dartFile, config) }
        val firstBindingOffset = PsiTreeUtil.findChildrenOfType(dartFile, DartClass::class.java)
            .filter { it.superClass?.text?.substringBefore('<')?.substringAfterLast('.')?.trim() == "Bindings" }
            .minOfOrNull { it.textRange.startOffset }
        val edits = mutableListOf<TextEdit>()

        if (missingImports.isNotEmpty()) {
            val imports = PsiTreeUtil.findChildrenOfType(dartFile, DartImportStatement::class.java)
            val offset = imports.maxOfOrNull { it.textRange.endOffset }
                ?: dartFile.children.filterIsInstance<DartLibraryStatement>().maxOfOrNull { it.textRange.endOffset }
                ?: 0
            val prefix = if (offset == 0) "" else "\n"
            val suffix = if (imports.isEmpty()) "\n" else ""
            edits += TextEdit(offset, prefix + missingImports.joinToString("\n") { "import '$it';" } + suffix, true)
        }
        if (repositoryHelper?.createRequired == true) {
            val offset = firstBindingOffset ?: document.textLength
            edits += TextEdit(
                offset,
                generator.repositoryRegistration(config, repositoryHelper.methodName) + "\n\n",
                true,
            )
        }
        val classPrefix = if (document.text.endsWith("\n\n")) "" else if (document.text.endsWith('\n')) "\n" else "\n\n"
        edits += TextEdit(
            document.textLength,
            classPrefix + generator.bindingClass(config, repositoryHelper?.methodName) + "\n",
            true,
        )

        val markers = mutableListOf<RangeMarker>()
        edits.sortedByDescending(TextEdit::offset).forEach { edit ->
            document.insertString(edit.offset, edit.text)
            if (edit.format) markers += document.createRangeMarker(edit.offset, edit.offset + edit.text.length)
        }
        documents.commitDocument(document)
        markers.filter(RangeMarker::isValid).forEach { marker ->
            CodeStyleManager.getInstance(project).reformatText(
                dartFile,
                listOf(TextRange(marker.startOffset, marker.endOffset)),
            )
        }
        documents.commitDocument(document)
    }

    private fun topLevelFunctions(dartFile: DartFile): Collection<DartFunctionDeclarationWithBodyOrNative> =
        PsiTreeUtil.findChildrenOfType(dartFile, DartFunctionDeclarationWithBodyOrNative::class.java)
            .filter { PsiTreeUtil.getParentOfType(it, DartClass::class.java) == null }

    private fun createCommonInfrastructure(
        libDirectory: PsiDirectory,
        config: RouteGenerateConfig,
        createdFiles: MutableList<PsiFile>,
    ) {
        val requiresPageControllerMixin = config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE &&
            config.controllerMode != ControllerMode.NONE
        if (!requiresPageControllerMixin && config.pageCapability != PageCapability.DATA_STATE) return
        val mixins = ensureDirectoryPath(libDirectory, "core", "mixins")
        if (requiresPageControllerMixin && mixins.findFile("page_controller_mixin.dart") == null) {
            createdFiles += createDartFile(
                mixins,
                "page_controller_mixin.dart",
                generator.pageControllerMixin(),
            )
        }
        if (config.pageCapability == PageCapability.DATA_STATE) {
            if (mixins.findFile("page_state_mixin.dart") == null) {
                createdFiles += createDartFile(mixins, "page_state_mixin.dart", generator.pageStateMixin())
            }
            val pageStateWidgets = ensureDirectoryPath(libDirectory, "core", "widgets", "page_state")
            if (pageStateWidgets.findFile("page_state_widgets.dart") == null) {
                createdFiles += createDartFile(
                    pageStateWidgets,
                    "page_state_widgets.dart",
                    generator.pageStateWidgets(),
                )
            }
        }
    }

    private fun ensureDirectoryPath(parent: PsiDirectory, vararg segments: String): PsiDirectory =
        segments.fold(parent, ::ensureDirectory)

    private fun resolveRepositoryHelper(dartFile: DartFile, config: RouteGenerateConfig): RepositoryHelper {
        val repositoryClass = requireNotNull(config.repositoryClassName)
        val expectedMethod = requireNotNull(config.repositoryRegistrationMethodName)
        val repositoryRegistration = "Get.lazyPut<$repositoryClass>"
        val functions = topLevelFunctions(dartFile)
        val expectedHelper = functions.firstOrNull { it.name == expectedMethod }
        if (expectedHelper != null) {
            if (repositoryRegistration !in expectedHelper.text) {
                throw GetXGenerationException(
                    "$expectedMethod() already exists but does not register $repositoryClass. " +
                        "Choose another binding file or adjust it manually.",
                )
            }
            return RepositoryHelper(expectedMethod, createRequired = false)
        }
        val legacyHelper = functions.firstOrNull { it.name == LEGACY_REPOSITORY_HELPER }
        if (legacyHelper != null && repositoryRegistration in legacyHelper.text) {
            return RepositoryHelper(LEGACY_REPOSITORY_HELPER, createRequired = false)
        }
        return RepositoryHelper(expectedMethod, createRequired = true)
    }

    private data class TextEdit(val offset: Int, val text: String, val format: Boolean)
    private data class RepositoryHelper(val methodName: String, val createRequired: Boolean)

    private companion object {
        const val LEGACY_REPOSITORY_HELPER = "_registerRepository"
    }
}
