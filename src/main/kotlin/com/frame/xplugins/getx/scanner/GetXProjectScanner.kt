package com.frame.xplugins.getx.scanner

import com.frame.xplugins.getx.model.BindingFileCandidate
import com.frame.xplugins.getx.model.DartClassCandidate
import com.frame.xplugins.getx.model.DartConstructorParameter
import com.frame.xplugins.getx.model.GetXScanResult
import com.frame.xplugins.getx.model.NavigationEvidence
import com.frame.xplugins.getx.model.PageControllerMixinCandidate
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.DartClass
import com.jetbrains.lang.dart.psi.DartFormalParameterList
import com.jetbrains.lang.dart.psi.DartMixinDeclaration
import com.jetbrains.lang.dart.psi.DartNormalFormalParameter
import com.jetbrains.lang.dart.psi.DartOptionalFormalParameters
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import com.jetbrains.lang.dart.psi.DartType

class GetXProjectScanner(private val project: Project) {

    fun scan(libDirectory: VirtualFile): GetXScanResult {
        val controllerCandidates = mutableListOf<DartClassCandidate>()
        val repositoryCandidates = mutableListOf<DartClassCandidate>()
        val bindingFiles = mutableListOf<BindingFileCandidate>()
        var pageControllerMixin: PageControllerMixinCandidate? = null
        val allClassNames = mutableSetOf<String>()
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

        FileTypeIndex.getFiles(DartFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            .asSequence()
            .sortedBy(VirtualFile::getPath)
            .forEach fileLoop@{ file ->
                val dartFile = psiManager.findFile(file) ?: return@fileLoop
                val sourceCode = codeWithoutCommentsAndStrings(dartFile)
                if (MATERIAL_APP_CALL.containsMatchIn(sourceCode)) hasMaterialApp = true
                if (NAVIGATOR_ROUTE_CALL.containsMatchIn(sourceCode)) hasNavigatorRoutes = true
                if (GET_MATERIAL_APP_CALL.containsMatchIn(sourceCode)) hasGetMaterialApp = true
                if (GETX_ROUTE_CALL.containsMatchIn(sourceCode)) hasGetXRoutes = true
                if (!VfsUtilCore.isAncestor(libDirectory, file, false)) return@fileLoop
                val classes = PsiTreeUtil.findChildrenOfType(dartFile, DartClass::class.java)
                val bindingClassNames = mutableSetOf<String>()
                classes.forEach classLoop@{ dartClass ->
                    val className = dartClass.name ?: return@classLoop
                    allClassNames += className
                    val superType = dartClass.superClass?.text.orEmpty()
                    val candidate = DartClassCandidate(
                        className = className,
                        file = file,
                        relativePath = VfsUtilCore.getRelativePath(file, libDirectory.parent, '/') ?: file.path,
                        pointer = smartPointers.createSmartPsiElementPointer(dartClass),
                        unnamedConstructorParameters = unnamedConstructorParameters(dartClass),
                    )
                    if (superType.substringBefore('<').substringAfterLast('.').trim() == "GetxController") {
                        controllerCandidates += candidate
                    }
                    if (repositoryDirectory != null && VfsUtilCore.isAncestor(repositoryDirectory, file, false) &&
                        className.endsWith("Repository")
                    ) {
                        repositoryCandidates += candidate
                    }
                    if (superType.substringBefore('<').substringAfterLast('.').trim() == "Bindings") {
                        bindingClassNames += className
                    }
                    if (file == pageControllerMixinFile &&
                        className == PAGE_CONTROLLER_MIXIN &&
                        dartClass is DartMixinDeclaration
                    ) {
                        pageControllerMixin = PageControllerMixinCandidate(
                            file = file,
                            relativePath = VfsUtilCore.getRelativePath(file, libDirectory.parent, '/') ?: file.path,
                            pointer = smartPointers.createSmartPsiElementPointer(dartClass),
                            compatibilityErrors = pageControllerMixinErrors(dartClass),
                        )
                    }
                }
                if (bindingDirectory != null && VfsUtilCore.isAncestor(bindingDirectory, file, false) &&
                    bindingClassNames.isNotEmpty()
                ) {
                    bindingFiles += BindingFileCandidate(
                        file = file,
                        relativePath = VfsUtilCore.getRelativePath(file, libDirectory.parent, '/') ?: file.path,
                        bindingClassNames = bindingClassNames,
                    )
                }
            }

        return GetXScanResult(
            controllers = controllerCandidates.sortedWith(compareBy(DartClassCandidate::className, DartClassCandidate::relativePath)),
            repositories = repositoryCandidates.sortedWith(compareBy(DartClassCandidate::className, DartClassCandidate::relativePath)),
            bindings = bindingFiles.sortedBy(BindingFileCandidate::relativePath),
            pageControllerMixin = pageControllerMixin,
            allClassNames = allClassNames,
            navigationEvidence = NavigationEvidence(
                hasMaterialApp = hasMaterialApp,
                hasNavigatorRoutes = hasNavigatorRoutes,
                hasGetMaterialApp = hasGetMaterialApp,
                hasGetXRoutes = hasGetXRoutes,
            ),
        )
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

    private fun codeWithoutCommentsAndStrings(root: PsiElement): String {
        val result = StringBuilder(root.text)
        val ignoredRanges = buildList {
            addAll(PsiTreeUtil.findChildrenOfType(root, PsiComment::class.java).map(PsiElement::getTextRange))
            addAll(
                PsiTreeUtil.findChildrenOfType(root, DartStringLiteralExpression::class.java)
                    .map(PsiElement::getTextRange),
            )
        }
        ignoredRanges.sortedByDescending { it.startOffset }.forEach { range ->
            result.replace(range.startOffset, range.endOffset, " ".repeat(range.length))
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
        const val PAGE_CONTROLLER_MIXIN = "PageControllerMixin"
    }
}
