package com.frame.xplugins.getx.dialog

import com.frame.xplugins.getx.model.BindingFileCandidate
import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.DartClassCandidate
import com.frame.xplugins.getx.model.GetXScanResult
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.naming.DartNamingConverter
import com.frame.xplugins.getx.path.FlutterPathContext
import com.frame.xplugins.getx.path.FlutterProjectPathResolver
import com.frame.xplugins.getx.service.GetXRouteGenerationService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.ButtonGroup
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class GetXRouteDialog(
    private val dialogProject: Project,
    private val pathContext: FlutterPathContext,
    private val scan: GetXScanResult,
) : DialogWrapper(dialogProject, true) {

    private val routeNameField = JBTextField()
    private val prefixField = JBTextField("tk")
    private val targetLabel = JBLabel()
    private val pageTypeCombo = ComboBox(PageType.entries.toTypedArray())
    private val routeNavigator = JBRadioButton(
        "Navigator + Page Scope",
        scan.navigationEvidence.recommendedMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE,
    )
    private val routeGetX = JBRadioButton(
        "GetX Route + Binding",
        scan.navigationEvidence.recommendedMode != RouteDiMode.NAVIGATOR_PAGE_SCOPE,
    )
    private val capabilityBasic = JBRadioButton("Basic", true)
    private val capabilityDataState = JBRadioButton("Data State")
    private val enablePullToRefresh = JBCheckBox("Enable Pull To Refresh", true)
    private val generateMockExample = JBCheckBox("Generate Mock Example", true)
    private val dataStateOptionsPanel = optionPanel(enablePullToRefresh, generateMockExample)

    private val controllerCreate = JBRadioButton("Create New", true)
    private val controllerExisting = JBRadioButton("Use Existing")
    private val controllerNone = JBRadioButton("None")
    private val controllerCombo = ComboBox(DefaultComboBoxModel(scan.controllers.toTypedArray()))

    private val repositoryCreate = JBRadioButton("Create New")
    private val repositoryExisting = JBRadioButton("Use Existing", scan.repositories.isNotEmpty())
    private val repositoryNone = JBRadioButton("None")
    private val repositoryCombo = ComboBox(DefaultComboBoxModel(scan.repositories.toTypedArray()))

    private val bindingCreate = JBRadioButton("Create New File")
    private val bindingAppend = JBRadioButton("Append Binding Class To Existing File", scan.bindings.isNotEmpty())
    private val bindingNone = JBRadioButton("None")
    private val bindingCombo = ComboBox(DefaultComboBoxModel(scan.bindings.toTypedArray()))
    private val dependencyAssemblyLayout = CardLayout()
    private val navigatorLifecycleLabel = JBLabel("PageControllerMixin (auto)")
    private val dependencyAssemblyPanel = JPanel(dependencyAssemblyLayout).apply {
        add(optionPanel(bindingAppend, bindingCreate, bindingNone, bindingCombo), GETX_BINDING_CARD)
        add(optionPanel(navigatorLifecycleLabel), NAVIGATOR_LIFECYCLE_CARD)
    }

    private val createWidgets = JBCheckBox("Create widgets directory", true)
    private val previewArea = JBTextArea()
    private val generationService = GetXRouteGenerationService()

    init {
        title = "Create GetX Route"
        setOKButtonText("Create")
        if (scan.repositories.isEmpty()) repositoryCreate.isSelected = true
        if (scan.bindings.isEmpty()) bindingCreate.isSelected = true
        ButtonGroup().apply {
            add(controllerCreate); add(controllerExisting); add(controllerNone)
        }
        ButtonGroup().apply {
            add(repositoryCreate); add(repositoryExisting); add(repositoryNone)
        }
        ButtonGroup().apply {
            add(bindingCreate); add(bindingAppend); add(bindingNone)
        }
        ButtonGroup().apply {
            add(capabilityBasic); add(capabilityDataState)
        }
        ButtonGroup().apply {
            add(routeNavigator); add(routeGetX)
        }
        previewArea.isEditable = false
        previewArea.lineWrap = false
        previewArea.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, previewArea.font.size)
        init()
        installListeners()
        updateState()
    }

    fun resultConfig(): RouteGenerateConfig = requireNotNull(buildConfig())

    override fun createCenterPanel(): JComponent {
        val controllerPanel = optionPanel(controllerCreate, controllerExisting, controllerNone, controllerCombo)
        val repositoryPanel = optionPanel(repositoryExisting, repositoryCreate, repositoryNone, repositoryCombo)
        val capabilityPanel = JPanel(BorderLayout()).apply {
            add(optionPanel(capabilityBasic, capabilityDataState), BorderLayout.NORTH)
            add(dataStateOptionsPanel, BorderLayout.CENTER)
        }
        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent("Route Name", routeNameField)
            .addLabeledComponent("Prefix", prefixField)
            .addLabeledComponent("Target", targetLabel)
            .addLabeledComponent("Route / DI Mode", optionPanel(routeNavigator, routeGetX))
            .addLabeledComponent("Page Capability", capabilityPanel)
            .addLabeledComponent("Page Type", pageTypeCombo)
            .addLabeledComponent("Controller", controllerPanel)
            .addLabeledComponent("Repository", repositoryPanel)
            .addLabeledComponent("Dependency Assembly", dependencyAssemblyPanel)
            .addLabeledComponent("Options", createWidgets)
            .panel
        val preview = JBScrollPane(previewArea).apply { preferredSize = Dimension(700, 230) }
        return JPanel(BorderLayout(0, JBUI.scale(12))).apply {
            border = JBUI.Borders.empty(8)
            add(form, BorderLayout.NORTH)
            add(JPanel(BorderLayout()).apply {
                add(JBLabel("Preview"), BorderLayout.NORTH)
                add(preview, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = routeNameField

    override fun doValidate(): ValidationInfo? {
        val config = try {
            buildConfig()
        } catch (error: IllegalArgumentException) {
            return ValidationInfo(error.message ?: "Invalid route configuration", routeNameField)
        }
        if (config == null) return ValidationInfo("Route Name is required", routeNameField)
        if (config.pageType == PageType.GET_VIEW && config.controllerMode == ControllerMode.NONE) {
            return ValidationInfo("GetView requires a controller", pageTypeCombo)
        }
        if (config.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING &&
            config.controllerMode == ControllerMode.CREATE &&
            config.bindingMode == BindingMode.NONE
        ) {
            return ValidationInfo("A newly created controller requires a binding", bindingNone)
        }
        if (config.controllerMode == ControllerMode.EXISTING && config.existingController == null) {
            return ValidationInfo("Select an existing controller", controllerCombo)
        }
        if (config.repositoryMode == RepositoryMode.EXISTING && config.existingRepository == null) {
            return ValidationInfo("Select an existing repository", repositoryCombo)
        }
        if (config.bindingMode == BindingMode.APPEND && config.existingBinding == null) {
            return ValidationInfo("Select an existing binding file", bindingCombo)
        }
        return null
    }

    override fun doOKAction() {
        val config = runCatching { buildConfig() }.getOrNull()
        if (config?.routeDiMode == RouteDiMode.GETX_ROUTE_BINDING &&
            !scan.navigationEvidence.hasGetXRouteInfrastructure
        ) {
            val result = Messages.showYesNoDialog(
                dialogProject,
                "Current project does not appear to use GetX route bindings.\n\n" +
                    "The generated GetView resolves its Controller through Get.find(), so the generated " +
                    "Binding must be executed before the page is built.\n\nContinue?",
                "GetX Route Binding Compatibility",
                Messages.getWarningIcon(),
            )
            if (result != Messages.YES) return
        }
        super.doOKAction()
    }

    private fun optionPanel(vararg components: JComponent): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
        components.forEach(::add)
    }

    private fun installListeners() {
        val documentListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = updateState()
        }
        routeNameField.document.addDocumentListener(documentListener)
        prefixField.document.addDocumentListener(documentListener)
        listOf(
            controllerCreate, controllerExisting, controllerNone,
            repositoryCreate, repositoryExisting, repositoryNone,
            bindingCreate, bindingAppend, bindingNone,
            capabilityBasic, capabilityDataState,
            routeNavigator, routeGetX,
        ).forEach { it.addActionListener { updateState() } }
        pageTypeCombo.addActionListener { updateState() }
        controllerCombo.addActionListener { updatePreview() }
        repositoryCombo.addActionListener { updatePreview() }
        bindingCombo.addActionListener { updatePreview() }
        createWidgets.addActionListener { updatePreview() }
        enablePullToRefresh.addActionListener { updatePreview() }
        generateMockExample.addActionListener { updatePreview() }
    }

    private fun updateState() {
        val isDataState = capabilityDataState.isSelected
        val isNavigator = routeNavigator.isSelected
        dataStateOptionsPanel.isVisible = isDataState
        if (isDataState) {
            controllerCreate.isSelected = true
        }
        val hasController = !controllerNone.isSelected
        navigatorLifecycleLabel.text = if (hasController) {
            "PageControllerMixin (auto)"
        } else {
            "No controller lifecycle infrastructure"
        }
        pageTypeCombo.isEnabled = !isDataState && (!isNavigator || !hasController)
        if (isNavigator && hasController) {
            pageTypeCombo.selectedItem = PageType.STATEFUL
        } else if (isDataState) {
            pageTypeCombo.selectedItem = PageType.GET_VIEW
        } else if (isNavigator && pageTypeCombo.selectedItem == PageType.GET_VIEW) {
            pageTypeCombo.selectedItem = PageType.STATELESS
        }
        controllerCreate.isEnabled = !isDataState
        controllerExisting.isEnabled = !isDataState && scan.controllers.isNotEmpty()
        controllerNone.isEnabled = !isDataState
        repositoryExisting.isEnabled = scan.repositories.isNotEmpty()
        bindingAppend.isEnabled = scan.bindings.isNotEmpty()
        dependencyAssemblyLayout.show(
            dependencyAssemblyPanel,
            if (isNavigator) NAVIGATOR_LIFECYCLE_CARD else GETX_BINDING_CARD,
        )
        if (controllerNone.isSelected) {
            repositoryNone.isSelected = true
            bindingNone.isSelected = true
            if (pageTypeCombo.selectedItem == PageType.GET_VIEW) pageTypeCombo.selectedItem = PageType.STATELESS
        } else if (!isNavigator && controllerCreate.isSelected && bindingNone.isSelected) {
            if (scan.bindings.isNotEmpty()) bindingAppend.isSelected = true else bindingCreate.isSelected = true
        }
        if (!isNavigator && controllerExisting.isSelected && bindingNone.isSelected) repositoryNone.isSelected = true
        if (isNavigator) bindingNone.isSelected = true
        val assemblesDependencies = hasController && (isNavigator || !bindingNone.isSelected)
        repositoryExisting.isEnabled = assemblesDependencies && scan.repositories.isNotEmpty()
        repositoryCreate.isEnabled = assemblesDependencies
        repositoryNone.isEnabled = hasController
        bindingCreate.isEnabled = !isNavigator && hasController
        bindingAppend.isEnabled = !isNavigator && hasController && scan.bindings.isNotEmpty()
        bindingNone.isEnabled = !isNavigator && (controllerExisting.isSelected || controllerNone.isSelected)
        controllerCombo.isEnabled = controllerExisting.isSelected && scan.controllers.isNotEmpty()
        repositoryCombo.isEnabled = repositoryExisting.isSelected && scan.repositories.isNotEmpty()
        bindingCombo.isEnabled = bindingAppend.isSelected && scan.bindings.isNotEmpty()
        selectRecommendations()
        updatePreview()
    }

    private fun selectRecommendations() {
        val route = runCatching { DartNamingConverter.routeName(routeNameField.text) }.getOrNull() ?: return
        val domain = FlutterProjectPathResolver.businessDomain(pathContext, route.snakeCase)
        val domainClass = DartNamingConverter.routeName(domain).pascalCase
        if (repositoryExisting.isSelected) {
            scan.repositories.firstOrNull { it.className == "${domainClass}Repository" }?.let {
                repositoryCombo.selectedItem = it
            }
        }
        if (bindingAppend.isSelected) {
            scan.bindings.firstOrNull { candidate ->
                candidate.file.nameWithoutExtension.endsWith("${domain}_binding") ||
                    "${domainClass}Binding" in candidate.bindingClassNames
            }?.let { bindingCombo.selectedItem = it }
        }
    }

    private fun updatePreview() {
        val config = runCatching { buildConfig() }.getOrNull()
        if (config == null) {
            targetLabel.text = "${pathContext.parentDirectory.path}/<route_name>"
            previewArea.text = "Enter a valid Route Name to preview changes."
            return
        }
        targetLabel.text = config.targetPath
        previewArea.text = generationService.plan(config).render()
        previewArea.caretPosition = 0
    }

    private fun buildConfig(): RouteGenerateConfig? {
        if (routeNameField.text.isBlank()) return null
        val route = DartNamingConverter.routeName(routeNameField.text)
        val prefix = DartNamingConverter.prefix(prefixField.text)
        val controllerMode = when {
            controllerCreate.isSelected -> ControllerMode.CREATE
            controllerExisting.isSelected -> ControllerMode.EXISTING
            else -> ControllerMode.NONE
        }
        val repositoryMode = when {
            repositoryCreate.isSelected -> RepositoryMode.CREATE
            repositoryExisting.isSelected -> RepositoryMode.EXISTING
            else -> RepositoryMode.NONE
        }
        val bindingMode = when {
            routeNavigator.isSelected -> BindingMode.NONE
            bindingCreate.isSelected -> BindingMode.CREATE
            bindingAppend.isSelected -> BindingMode.APPEND
            else -> BindingMode.NONE
        }
        val routeDiMode = if (routeNavigator.isSelected) {
            RouteDiMode.NAVIGATOR_PAGE_SCOPE
        } else {
            RouteDiMode.GETX_ROUTE_BINDING
        }
        return RouteGenerateConfig(
            parentDirectory = pathContext.parentDirectory,
            libDirectory = pathContext.libDirectory,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = prefix,
            businessDomain = FlutterProjectPathResolver.businessDomain(pathContext, route.snakeCase),
            pageType = pageTypeCombo.selectedItem as PageType,
            controllerMode = controllerMode,
            existingController = if (controllerMode == ControllerMode.EXISTING) controllerCombo.selectedItem as? DartClassCandidate else null,
            repositoryMode = repositoryMode,
            existingRepository = if (repositoryMode == RepositoryMode.EXISTING) repositoryCombo.selectedItem as? DartClassCandidate else null,
            bindingMode = bindingMode,
            existingBinding = if (bindingMode == BindingMode.APPEND) bindingCombo.selectedItem as? BindingFileCandidate else null,
            createWidgetsDirectory = createWidgets.isSelected,
            pageCapability = if (capabilityDataState.isSelected) PageCapability.DATA_STATE else PageCapability.BASIC,
            enablePullToRefresh = enablePullToRefresh.isSelected,
            generateMockExample = generateMockExample.isSelected,
            routeDiMode = routeDiMode,
        )
    }

    private companion object {
        const val GETX_BINDING_CARD = "getx-binding"
        const val NAVIGATOR_LIFECYCLE_CARD = "navigator-lifecycle"
    }
}
