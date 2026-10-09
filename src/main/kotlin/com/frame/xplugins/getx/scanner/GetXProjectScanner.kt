package com.frame.xplugins.getx.scanner

import com.frame.xplugins.getx.model.BindingFileCandidate
import com.frame.xplugins.getx.model.DartClassCandidate
import com.frame.xplugins.getx.model.DartConstructorParameter
import com.frame.xplugins.getx.model.GetXScanResult
import com.frame.xplugins.getx.model.NavigationEvidence
import com.frame.xplugins.getx.model.PageControllerMixinCandidate
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.lang.dart.ide.index.DartClassIndex
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.DartTokenTypesSets
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.lexer.DartLexer
import com.jetbrains.lang.dart.psi.DartClass
import com.jetbrains.lang.dart.psi.DartFormalParameterList
import com.jetbrains.lang.dart.psi.DartMixinDeclaration
import com.jetbrains.lang.dart.psi.DartNormalFormalParameter
import com.jetbrains.lang.dart.psi.DartOptionalFormalParameters
import com.jetbrains.lang.dart.psi.DartType

class GetXProjectScanner(private val project: Project) {

    fun scan(libDirectory: VirtualFile, controllerSearchDirectory: VirtualFile = libDirectory): GetXScanResult {
        val controllerCandidates = mutableListOf<DartClassCandidate>()
        val repositoryCandidates = mutableListOf<DartClassCandidate>()
        val bindingFiles = mutableListOf<BindingFileCandidate>()
        var pageControllerMixin: PageControllerMixinCandidate? = null
        var hasMaterialApp = false
        var hasNavigatorRoutes = false
        var hasGetMaterialApp = false
        var hasGetXRoutes = false
        val psiManager = PsiManager.getInstance(project)
        val smartPointers = SmartPointerManager.getInstance(project)
        val repositoryDirectory = libDirectory.findChild("repositories")
        val bindingDirectory = libDirectory.findChild("bindings")
        val pageControllerMixinFile = libDirectory.findFileByRelativePath(
            "core/mixins/page_controller_mixin.dart",
        )
        val flutterRoot = libDirectory.parent ?: libDirectory
        val controllerRoots = buildSet {
            if (controllerSearchDirectory.isDirectory) add(controllerSearchDirectory)
            libDirectory.findChild("controllers")?.takeIf(VirtualFile::isDirectory)?.let(::add)
        }
        dartFiles(controllerRoots).filter(::isControllerFile).forEach { file ->
            ProgressManager.checkCanceled()
            val dartFile = psiManager.findFile(file) ?: return@forEach
            PsiTreeUtil.getChildrenOfTypeAsList(dartFile, DartClass::class.java).forEach classLoop@{ dartClass ->
                if (dartClass.name == null) return@classLoop
                val superType = dartClass.superClass?.text.orEmpty()
                if (superType.substringBefore('<').substringAfterLast('.').trim() != "GetxController") {
                    return@classLoop
                }
                controllerCandidates += candidate(dartClass, file, libDirectory, smartPointers)
            }
        }

        if (repositoryDirectory?.isDirectory == true) {
            dartFiles(setOf(repositoryDirectory)).forEach { file ->
                ProgressManager.checkCanceled()
                val dartFile = psiManager.findFile(file) ?: return@forEach
                PsiTreeUtil.getChildrenOfTypeAsList(dartFile, DartClass::class.java)
                    .filter { it.name?.endsWith("Repository") == true }
                    .forEach { repositoryCandidates += candidate(it, file, libDirectory, smartPointers) }
            }
        }

        if (bindingDirectory?.isDirectory == true) {
            dartFiles(setOf(bindingDirectory)).forEach { file ->
                ProgressManager.checkCanceled()
                val dartFile = psiManager.findFile(file) ?: return@forEach
                val bindingClassNames = PsiTreeUtil.getChildrenOfTypeAsList(dartFile, DartClass::class.java)
                    .filter {
                        it.superClass?.text.orEmpty().substringBefore('<').substringAfterLast('.').trim() == "Bindings"
                    }
                    .mapNotNull(DartClass::getName)
                    .toSet()
                if (bindingClassNames.isNotEmpty()) {
                    bindingFiles += BindingFileCandidate(
                        file = file,
                        relativePath = relativePath(file, libDirectory),
                        bindingClassNames = bindingClassNames,
                    )
                }
            }
        }

        if (pageControllerMixinFile?.isValid == true && !pageControllerMixinFile.isDirectory) {
            val dartFile = psiManager.findFile(pageControllerMixinFile)
            PsiTreeUtil.getChildrenOfTypeAsList(dartFile, DartClass::class.java)
                .filterIsInstance<DartMixinDeclaration>()
                .firstOrNull { it.name == PAGE_CONTROLLER_MIXIN }
                ?.let { mixin ->
                    pageControllerMixin = PageControllerMixinCandidate(
                        file = pageControllerMixinFile,
                        relativePath = relativePath(pageControllerMixinFile, libDirectory),
                        pointer = smartPointers.createSmartPsiElementPointer(mixin),
                        compatibilityErrors = pageControllerMixinErrors(mixin),
                    )
                }
        }

        navigationFiles(libDirectory, flutterRoot).forEach { file ->
            ProgressManager.checkCanceled()
            val rawSource = VfsUtilCore.loadText(file)
            if (NAVIGATION_HINTS.none(rawSource::contains)) return@forEach
            val sourceCode = codeWithoutCommentsAndStrings(rawSource)
            if (MATERIAL_APP_CALL.containsMatchIn(sourceCode)) hasMaterialApp = true
            if (NAVIGATOR_ROUTE_CALL.containsMatchIn(sourceCode)) hasNavigatorRoutes = true
            if (GET_MATERIAL_APP_CALL.containsMatchIn(sourceCode)) hasGetMaterialApp = true
            if (GETX_ROUTE_CALL.containsMatchIn(sourceCode)) hasGetXRoutes = true
        }

        return GetXScanResult(
            controllers = controllerCandidates.sortedWith(compareBy(DartClassCandidate::className, DartClassCandidate::relativePath)),
            repositories = repositoryCandidates.sortedWith(compareBy(DartClassCandidate::className, DartClassCandidate::relativePath)),
            bindings = bindingFiles.sortedBy(BindingFileCandidate::relativePath),
            pageControllerMixin = pageControllerMixin,
            navigationEvidence = NavigationEvidence(
                hasMaterialApp = hasMaterialApp,
                hasNavigatorRoutes = hasNavigatorRoutes,
                hasGetMaterialApp = hasGetMaterialApp,
                hasGetXRoutes = hasGetXRoutes,
            ),
        )
    }

    fun containsClassName(className: String, libDirectory: VirtualFile): Boolean {
        val scope: GlobalSearchScope = GlobalSearchScopesCore.directoryScope(project, libDirectory, true)
        return FileBasedIndex.getInstance().getContainingFiles(
            DartClassIndex.DART_CLASS_INDEX,
            className,
            scope,
        ).isNotEmpty()
    }

    fun refreshCandidate(candidate: DartClassCandidate): DartClassCandidate? {
        if (!candidate.file.isValid) return null
        val dartClass = candidate.pointer.element?.takeIf { it.isValid && it.name == candidate.className } ?: return null
        return candidate.copy(unnamedConstructorParameters = unnamedConstructorParameters(dartClass))
    }

    fun refreshPageControllerMixin(candidate: PageControllerMixinCandidate): PageControllerMixinCandidate? {
        if (!candidate.file.isValid) return null
        val mixin = candidate.pointer.element?.takeIf { it.isValid && it.name == PAGE_CONTROLLER_MIXIN }
            ?: return null
        return candidate.copy(compatibilityErrors = pageControllerMixinErrors(mixin))
    }

    private fun candidate(
        dartClass: DartClass,
        file: VirtualFile,
        libDirectory: VirtualFile,
        smartPointers: SmartPointerManager,
    ) = DartClassCandidate(
        className = requireNotNull(dartClass.name),
        file = file,
        relativePath = relativePath(file, libDirectory),
        pointer = smartPointers.createSmartPsiElementPointer(dartClass),
        unnamedConstructorParameters = unnamedConstructorParameters(dartClass),
    )

    private fun relativePath(file: VirtualFile, libDirectory: VirtualFile): String =
        VfsUtilCore.getRelativePath(file, libDirectory.parent, '/') ?: file.path

    private fun isControllerFile(file: VirtualFile): Boolean =
        file.nameWithoutExtension.endsWith("controller", ignoreCase = true)

    private fun dartFiles(roots: Set<VirtualFile>): List<VirtualFile> = buildList {
        val visited = mutableSetOf<String>()
        val visitedDirectories = mutableSetOf<String>()
        fun visit(directory: VirtualFile) {
            if (!visitedDirectories.add(directory.path)) return
            ProgressManager.checkCanceled()
            directory.children.forEach { child ->
                if (child.isDirectory) visit(child)
                else if (child.extension.equals("dart", ignoreCase = true) && visited.add(child.path)) add(child)
            }
        }
        roots.forEach(::visit)
    }.sortedBy(VirtualFile::getPath)

    private fun navigationFiles(libDirectory: VirtualFile, flutterRoot: VirtualFile): List<VirtualFile> {
        val files = linkedSetOf<VirtualFile>()
        listOf("main.dart", "app.dart").forEach { name ->
            libDirectory.findChild(name)?.takeIf { !it.isDirectory }?.let(files::add)
            flutterRoot.findFileByRelativePath("example/lib/$name")?.takeIf { !it.isDirectory }?.let(files::add)
        }
        libDirectory.children
            .filter { !it.isDirectory && it.extension.equals("dart", ignoreCase = true) }
            .filter { file ->
                val name = file.nameWithoutExtension.lowercase()
                listOf("route", "router", "navigation").any(name::contains)
            }
            .forEach(files::add)
        val navigationRoots = listOf("routes", "router", "navigation", "config", "utils")
            .mapNotNull(libDirectory::findChild)
            .filter(VirtualFile::isDirectory)
            .toSet()
        dartFiles(navigationRoots)
            .filter { file ->
                val name = file.nameWithoutExtension.lowercase()
                listOf("route", "router", "navigation").any(name::contains) ||
                    navigationRoots.any { root ->
                        root.name in setOf("routes", "router", "navigation") &&
                            VfsUtilCore.isAncestor(root, file, false)
                    }
            }
            .forEach(files::add)
        return files.sortedBy(VirtualFile::getPath)
    }

    private fun unnamedConstructorParameters(dartClass: DartClass): List<DartConstructorParameter>? {
        val constructors = dartClass.constructors
        if (constructors.isEmpty()) return emptyList()
        val className = dartClass.name ?: return null
        val unnamed = constructors.firstOrNull { constructor ->
            constructor.name == className || constructor.nameIdentifier?.text == className
        } ?: return null
        val parameters = PsiTreeUtil.findChildOfType(unnamed, DartFormalParameterList::class.java)
            ?: return emptyList()
        return PsiTreeUtil.findChildrenOfType(parameters, DartNormalFormalParameter::class.java)
            .map { parameter ->
                val fieldParameter = parameter.fieldFormalParameter
                val resolvedField = fieldParameter?.referenceExpression?.resolve()
                val fieldName = fieldParameter?.referenceExpression?.text?.substringAfterLast('.')
                val declaredField = fieldName?.let(dartClass::findFieldByName)
                val typeText = parameter.simpleFormalParameter?.type?.text
                    ?: fieldParameter?.type?.text
                    ?: resolvedField?.let { PsiTreeUtil.findChildOfType(it, DartType::class.java)?.text }
                    ?: declaredField?.let { PsiTreeUtil.findChildOfType(it, DartType::class.java)?.text }
                DartConstructorParameter(
                    typeText = typeText,
                    sourceText = parameter.text,
                    isOptional = PsiTreeUtil.getParentOfType(
                        parameter,
                        DartOptionalFormalParameters::class.java,
                        true,
                        DartFormalParameterList::class.java,
                    ) != null,
                )
            }
    }

    private fun codeWithoutCommentsAndStrings(source: String): String {
        val result = StringBuilder(source)
        val lexer = DartLexer()
        var stringDepth = 0
        lexer.start(source)
        while (lexer.tokenType != null) {
            val tokenType = lexer.tokenType
            if (tokenType == DartTokenTypes.OPEN_QUOTE) stringDepth++
            val shouldMask = stringDepth > 0 ||
                DartTokenTypesSets.COMMENTS.contains(tokenType) ||
                DartTokenTypesSets.STRINGS.contains(tokenType)
            if (shouldMask) {
                for (index in lexer.tokenStart until lexer.tokenEnd) {
                    if (result[index] != '\n' && result[index] != '\r') result.setCharAt(index, ' ')
                }
            }
            if (tokenType == DartTokenTypes.CLOSING_QUOTE && stringDepth > 0) stringDepth--
            lexer.advance()
        }
        return result.toString()
    }

    private fun pageControllerMixinErrors(mixin: DartMixinDeclaration): List<String> {
        val parameters = mixin.typeParameters?.typeParameterList.orEmpty()
        val widgetType = parameters.getOrNull(0)?.name
        val controllerType = parameters.getOrNull(1)?.name
        val text = mixin.text
        return buildList {
            if (parameters.size != 2 || widgetType == null || controllerType == null) {
                add("PageControllerMixin must declare Widget and Controller type parameters.")
                return@buildList
            }
            if (parameters[0].type?.text?.substringAfterLast('.') != "StatefulWidget") {
                add("The Widget type must extend StatefulWidget.")
            }
            if (parameters[1].type?.text?.substringAfterLast('.') != "GetxController") {
                add("The Controller type must extend GetxController.")
            }
            val onType = mixin.onMixins?.typeList?.text.orEmpty().replace(Regex("\\s+"), "")
            if ("State<$widgetType>" !in onType) add("PageControllerMixin must apply on State<$widgetType>.")
            val hasLegacyControllerField =
                Regex("late\\s+final\\s+$controllerType\\s+controller\\s*;").containsMatchIn(text)
            val privateControllerName = Regex(
                "late\\s+final\\s+$controllerType\\s+(_[A-Za-z][A-Za-z0-9_]*)\\s*;",
            ).find(text)?.groupValues?.get(1)
            val privateControllerStorage = privateControllerName.orEmpty()
            val usesPrivateController = !hasLegacyControllerField && privateControllerName != null
            if (!hasLegacyControllerField && privateControllerName == null) {
                add("PageControllerMixin must own a late final $controllerType Controller instance.")
            } else if (usesPrivateController && !Regex(
                    "$controllerType\\s+get\\s+controller\\s*=>\\s*${Regex.escape(privateControllerStorage)}\\s*;",
                ).containsMatchIn(text)
            ) {
                add("PageControllerMixin must expose its private Controller through a read-only controller getter.")
            }
            if (!Regex("$controllerType\\s+createController\\s*\\(\\s*\\)\\s*;").containsMatchIn(text)) {
                add("PageControllerMixin must declare $controllerType createController().")
            }
            val initState = mixin.findMethodByName("initState")
            val dispose = mixin.findMethodByName("dispose")
            val tagName = Regex("late\\s+final\\s+String\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;")
                .find(text)
                ?.groupValues
                ?.get(1)
            val legacyTagAssignment = tagName != null && Regex(
                "$tagName\\s*=\\s*UniqueKey\\s*\\(\\s*\\)\\.toString\\s*\\(\\s*\\)",
            ).containsMatchIn(text)
            val typedTagLiterals = listOf(
                "'" + "$" + controllerType + "-" + "$" + "{UniqueKey()}'",
                "\"" + "$" + controllerType + "-" + "$" + "{UniqueKey()}\"",
            )
            val typedTagAssignment = tagName != null && typedTagLiterals.any { literal ->
                Regex("$tagName\\s*=\\s*${Regex.escape(literal)}").containsMatchIn(text)
            }
            if (tagName == null || (!legacyTagAssignment && !typedTagAssignment)) {
                add("PageControllerMixin must create a unique Controller tag.")
            } else {
                val getPutPattern =
                    "Get\\.put\\s*<$controllerType>\\s*\\(\\s*createController\\s*\\(\\s*\\)\\s*," +
                        "[\\s\\S]*?tag\\s*:\\s*$tagName"
                val registrationPattern = if (usesPrivateController) {
                    "${Regex.escape(privateControllerStorage)}\\s*=\\s*$getPutPattern"
                } else {
                    getPutPattern
                }
                if (!Regex(registrationPattern).containsMatchIn(initState?.text.orEmpty())) {
                    add("PageControllerMixin must register createController() with its unique tag.")
                }
                if (!Regex("Get\\.delete\\s*<$controllerType>[\\s\\S]*?tag\\s*:\\s*$tagName")
                        .containsMatchIn(dispose?.text.orEmpty())
                ) {
                    add("PageControllerMixin must delete the Controller with the same tag.")
                }
                if (usesPrivateController) {
                    if (!typedTagAssignment) {
                        add("PageControllerMixin private Controller tags must include the Controller type.")
                    }
                    if (!Regex(
                            "@protected\\s+String\\s+get\\s+controllerTag\\s*=>\\s*$tagName\\s*;",
                        ).containsMatchIn(text)
                    ) {
                        add("PageControllerMixin must expose controllerTag through a protected getter.")
                    }
                    if (!Regex(
                            "Get\\.isRegistered\\s*<$controllerType>\\s*\\([\\s\\S]*?tag\\s*:\\s*$tagName",
                        ).containsMatchIn(dispose?.text.orEmpty())
                    ) {
                        add("PageControllerMixin must guard Controller deletion with Get.isRegistered.")
                    }
                }
            }
            if (initState == null || "super.initState();" !in initState.text) {
                add("PageControllerMixin must preserve the initState lifecycle chain.")
            } else if (initState.getMetadataByName("mustCallSuper") == null) {
                add("PageControllerMixin initState must be annotated with @mustCallSuper.")
            }
            if (dispose == null || "super.dispose();" !in dispose.text) {
                add("PageControllerMixin must preserve the dispose lifecycle chain.")
            } else if (dispose.getMetadataByName("mustCallSuper") == null) {
                add("PageControllerMixin dispose must be annotated with @mustCallSuper.")
            }
            if (".onStart(" in text || ".onDelete(" in text) {
                add("PageControllerMixin must not call Controller lifecycle methods directly.")
            }
        }
    }

    private companion object {
        val MATERIAL_APP_CALL = Regex("\\bMaterialApp\\s*\\(")
        val NAVIGATOR_ROUTE_CALL = Regex(
            "Navigator(?:\\.of\\([^)]*\\))?\\.(?:push|pushReplacement|pushAndRemoveUntil)\\s*\\(|" +
                "(?:Material|Cupertino)PageRoute\\s*\\(",
        )
        val GET_MATERIAL_APP_CALL = Regex("\\bGetMaterialApp\\s*\\(")
        val GETX_ROUTE_CALL = Regex("\\bGetPage\\s*\\(|\\bGet\\.(?:to|toNamed|off|offNamed)\\s*\\(")
        val NAVIGATION_HINTS = listOf("MaterialApp", "Navigator", "PageRoute", "GetPage", "Get.to", "Get.off")
        const val PAGE_CONTROLLER_MIXIN = "PageControllerMixin"
    }
}
