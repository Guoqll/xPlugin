package com.frame.xplugins.getx.generator

import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.path.FlutterProjectPathResolver

class GetXTemplateLoader {
    fun load(path: String): String = requireNotNull(javaClass.getResourceAsStream("/code/getx/$path")) {
        "GetX template not found: $path"
    }.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

class GetXCodeGenerator(private val templates: GetXTemplateLoader = GetXTemplateLoader()) {

    fun page(config: RouteGenerateConfig): String {
        if (config.routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE && config.controllerMode != ControllerMode.NONE) {
            return navigatorPage(config)
        }
        if (config.pageCapability == PageCapability.DATA_STATE) return getXDataStatePage(config)
        val hasController = config.controllerMode != ControllerMode.NONE
        val template = when (config.pageType) {
            PageType.GET_VIEW -> "page/get_view.dart.tpl"
            PageType.STATELESS -> if (hasController) "page/stateless_with_controller.dart.tpl" else "page/stateless.dart.tpl"
            PageType.STATEFUL -> if (hasController) "page/stateful_with_controller.dart.tpl" else "page/stateful.dart.tpl"
        }
        return render(
            templates.load(template),
            mapOf(
                "PAGE_CLASS" to config.pageClassName,
                "CONTROLLER_CLASS" to config.controllerClassName,
                "CONTROLLER_IMPORT" to controllerImportForPage(config),
                "BUSINESS_MIXIN_IMPORT" to businessMixinImport(config),
                "BUSINESS_MIXIN_DECLARATION" to businessMixinDeclaration(config),
            ),
        )
    }

    fun controller(config: RouteGenerateConfig): String {
        if (config.pageCapability == PageCapability.DATA_STATE) return dataStateController(config)
        val repositoryClass = config.repositoryClassName
        val template = if (repositoryClass == null) {
            "controller/without_repository.dart.tpl"
        } else {
            "controller/with_repository.dart.tpl"
        }
        return render(
            templates.load(template),
            mapOf(
                "CONTROLLER_CLASS" to config.controllerClassName,
                "REPOSITORY_CLASS" to (repositoryClass ?: ""),
                "REPOSITORY_IMPORT" to repositoryImportForController(config),
            ),
        )
    }

    fun repository(config: RouteGenerateConfig): String = render(
        templates.load("repository/repository.dart.tpl"),
        mapOf("REPOSITORY_CLASS" to requireNotNull(config.repositoryClassName)),
    )

    fun pageStateMixin(): String = render(templates.load("common/page_state_mixin.dart.tpl"), emptyMap())

    fun pageStateWidgets(): String = render(templates.load("common/page_state_widgets.dart.tpl"), emptyMap())

    fun pageControllerMixin(): String = render(templates.load("common/page_controller_mixin.dart.tpl"), emptyMap())

    fun businessMixin(config: RouteGenerateConfig): String {
        val mixin = requireNotNull(config.businessMixinSpec)
        return render(
            templates.load(mixin.templatePath),
            mapOf(
                "ACTIONS_MIXIN_CLASS" to mixin.mixinClassName,
                "CONTROLLER_CLASS" to mixin.controllerClassName,
                "CONTROLLER_IMPORT" to controllerImportForPage(config),
                "PAGE_CLASS" to config.pageClassName,
            ),
        )
    }

    fun bindingFile(config: RouteGenerateConfig): String {
        val imports = bindingImports(config).joinToString("\n") { "import '$it';" }
        val helper = if (config.repositoryClassName != null) repositoryRegistration(config) + "\n\n" else ""
        return "$imports\n\n$helper${bindingClass(config)}"
    }

    fun bindingClass(
        config: RouteGenerateConfig,
        repositoryRegistrationMethod: String? = config.repositoryRegistrationMethodName,
    ): String {
        val template = if (config.repositoryClassName == null) {
            "binding/without_repository.dart.tpl"
        } else {
            "binding/with_repository.dart.tpl"
        }
        return render(
            templates.load(template),
            mapOf(
                "BINDING_CLASS" to config.bindingClassName,
                "PAGE_CLASS" to config.pageClassName,
                "CONTROLLER_CLASS" to config.controllerClassName,
                "REPOSITORY_CLASS" to (config.repositoryClassName ?: ""),
                "REGISTER_REPOSITORY_METHOD" to (repositoryRegistrationMethod ?: ""),
            ),
        ).trimEnd()
    }

    fun repositoryRegistration(
        config: RouteGenerateConfig,
        methodName: String = requireNotNull(config.repositoryRegistrationMethodName),
    ): String = render(
        templates.load("binding/register_repository.dart.tpl"),
        mapOf(
            "REPOSITORY_CLASS" to requireNotNull(config.repositoryClassName),
            "REGISTER_REPOSITORY_METHOD" to methodName,
            "REPOSITORY_EXPRESSION" to repositoryExpression(config),
        ),
    ).trimEnd()

    fun bindingImports(config: RouteGenerateConfig): List<String> {
        val bindingDirectory = "${config.libDirectory.path}/bindings"
        val controllerPath = when (config.controllerMode) {
            com.frame.xplugins.getx.model.ControllerMode.CREATE -> "${config.targetPath}/${config.controllerFileName}"
            com.frame.xplugins.getx.model.ControllerMode.EXISTING -> requireNotNull(config.existingController).file.path
            com.frame.xplugins.getx.model.ControllerMode.NONE -> error("Binding requires a controller")
        }
        return buildList {
            add("package:get/get.dart")
            add(FlutterProjectPathResolver.relativeImport(bindingDirectory, controllerPath))
            val repositoryPath = when (config.repositoryMode) {
                com.frame.xplugins.getx.model.RepositoryMode.CREATE -> "${config.libDirectory.path}/repositories/${config.repositoryFileName}"
                com.frame.xplugins.getx.model.RepositoryMode.EXISTING -> requireNotNull(config.existingRepository).file.path
                com.frame.xplugins.getx.model.RepositoryMode.NONE -> null
            }
            if (repositoryPath != null) {
                if (repositoryRequiresTkApi(config)) add("../api/tk_api.dart")
                add(FlutterProjectPathResolver.relativeImport(bindingDirectory, repositoryPath))
            }
        }
    }

    private fun controllerImportForPage(config: RouteGenerateConfig): String = when (config.controllerMode) {
        ControllerMode.CREATE -> config.controllerFileName
        ControllerMode.EXISTING ->
            FlutterProjectPathResolver.relativeImport(config.targetPath, requireNotNull(config.existingController).file.path)
        ControllerMode.NONE -> ""
    }

    private fun repositoryImportForController(config: RouteGenerateConfig): String {
        val repositoryPath = when (config.repositoryMode) {
            RepositoryMode.CREATE ->
                "${config.libDirectory.path}/repositories/${config.repositoryFileName}"
            RepositoryMode.EXISTING -> requireNotNull(config.existingRepository).file.path
            RepositoryMode.NONE -> return ""
        }
        return FlutterProjectPathResolver.relativeImport(config.targetPath, repositoryPath)
    }

    private fun getXDataStatePage(config: RouteGenerateConfig): String = render(
        templates.load("page/data_state_get_view.dart.tpl"),
        mapOf(
            "PAGE_CLASS" to config.pageClassName,
            "CONTROLLER_CLASS" to config.controllerClassName,
            "CONTROLLER_IMPORT" to controllerImportForPage(config),
            "PAGE_STATE_WIDGETS_IMPORT" to FlutterProjectPathResolver.relativeImport(
                config.targetPath,
                config.pageStateWidgetsPath,
            ),
            "CONTENT_BODY" to dataStateContentBody(config),
        ),
    )

    private fun navigatorPage(config: RouteGenerateConfig): String {
        val template = if (config.pageCapability == PageCapability.DATA_STATE) {
            "page/navigator_data_state.dart.tpl"
        } else {
            "page/navigator_basic_with_controller.dart.tpl"
        }
        return render(
            templates.load(template),
            mapOf(
                "IMPORTS" to navigatorImports(config),
                "PAGE_CLASS" to config.pageClassName,
                "CONTROLLER_CLASS" to config.controllerClassName,
                "CONTROLLER_CREATION_BODY" to navigatorControllerCreationBody(config),
                "CONTENT_BODY" to dataStateContentBody(config),
                "BUSINESS_MIXIN_COMPOSITION" to businessMixinComposition(config),
            ),
        )
    }

    private fun navigatorImports(config: RouteGenerateConfig): String = buildList {
        add("package:flutter/material.dart")
        if (config.pageCapability == PageCapability.DATA_STATE) add("package:get/get.dart")
        add(FlutterProjectPathResolver.relativeImport(config.targetPath, config.pageControllerMixinPath))
        if (config.pageCapability == PageCapability.DATA_STATE) {
            add(FlutterProjectPathResolver.relativeImport(config.targetPath, config.pageStateWidgetsPath))
        }
        if (repositoryRequiresTkApi(config)) {
            add(FlutterProjectPathResolver.relativeImport(config.targetPath, "${config.libDirectory.path}/api/tk_api.dart"))
        }
        add(controllerImportForPage(config))
        repositoryPath(config)?.let { add(FlutterProjectPathResolver.relativeImport(config.targetPath, it)) }
        config.businessMixinFileName?.let(::add)
    }.distinct().joinToString("\n") { "import '$it';" }

    private fun businessMixinImport(config: RouteGenerateConfig): String =
        config.businessMixinFileName?.let { "import '$it';\n\n" }.orEmpty()

    private fun businessMixinDeclaration(config: RouteGenerateConfig): String =
        config.businessMixinSpec?.let { "\n    with ${it.mixinClassName}<${config.pageClassName}>" }.orEmpty()

    private fun businessMixinComposition(config: RouteGenerateConfig): String =
        config.businessMixinSpec?.let { ",\n        ${it.mixinClassName}<${config.pageClassName}>" }.orEmpty()

    private fun navigatorControllerCreationBody(config: RouteGenerateConfig): String {
        val repository = config.repositoryClassName?.let { repositoryExpression(config) }
        return if (repository == null) {
            "    return ${config.controllerClassName}();"
        } else {
            """
                |    return ${config.controllerClassName}(
                |      $repository,
                |    );
            """.trimMargin()
        }
    }

    private fun dataStateContentBody(config: RouteGenerateConfig): String =
        if (config.generateMockExample) {
            """
                |    return Obx(() {
                |      if (controller.items.isEmpty) {
                |        return PagePlaceholderView(
                |          icon: Icons.inbox_outlined,
                |          title: '暂无数据',
                |          message: '暂时没有可展示的内容',
                |          actionText: '重新加载',
                |          onAction: controller.loadData,
                |        );
                |      }
                |
                |      return ListView.separated(
                |        physics: const AlwaysScrollableScrollPhysics(),
                |        padding: const EdgeInsets.all(16),
                |        itemCount: controller.items.length,
                |        separatorBuilder: (_, __) => const SizedBox(height: 12),
                |        itemBuilder: (context, index) {
                |          return Card(
                |            margin: EdgeInsets.zero,
                |            child: ListTile(
                |              leading: const Icon(Icons.route_outlined),
                |              title: Text(controller.items[index]),
                |            ),
                |          );
                |        },
                |      );
                |    });
            """.trimMargin()
        } else {
            "    return const SizedBox();"
        }

    private fun dataStateController(config: RouteGenerateConfig): String {
        val hasRepository = config.repositoryClassName != null
        val template = if (hasRepository) {
            "controller/data_state_with_repository.dart.tpl"
        } else {
            "controller/data_state_without_repository.dart.tpl"
        }
        val businessState = if (config.generateMockExample) "  final items = <String>[].obs;\n" else ""
        val loadDataBody = if (config.generateMockExample) {
            """
                |      final result = await _mockRequest();
                |
                |      items.assignAll(result);
            """.trimMargin()
        } else {
            "      // TODO: 请求并处理页面数据\n"
        }
        val refreshBody = if (config.generateMockExample) {
            """
                |      final result = await _mockRequest();
                |
                |      items.assignAll(result);
            """.trimMargin()
        } else {
            "      // TODO: 请求并处理刷新数据\n"
        }
        val privateHelpers = if (config.generateMockExample) {
            """
                |
                |  /// 模拟网络请求。
                |  /// TODO: 接入真实接口后删除该方法。
                |  Future<List<String>> _mockRequest() async {
                |    await Future.delayed(const Duration(milliseconds: 800));
                |
                |    // return [];
                |    // throw Exception('模拟请求失败');
                |
                |    return [
                |      '${config.routeName}：${'$'}{_formatNow()}',
                |    ];
                |  }
                |
                |  String _formatNow() {
                |    final now = DateTime.now();
                |    String two(int value) => value.toString().padLeft(2, '0');
                |
                |    return '${'$'}{now.year}-${'$'}{two(now.month)}-${'$'}{two(now.day)} '
                |        '${'$'}{two(now.hour)}:${'$'}{two(now.minute)}:${'$'}{two(now.second)}';
                |  }
            """.trimMargin()
        } else {
            ""
        }
        return render(
            templates.load(template),
            mapOf(
                "CONTROLLER_CLASS" to config.controllerClassName,
                "REPOSITORY_CLASS" to (config.repositoryClassName ?: ""),
                "REPOSITORY_IMPORT" to repositoryImportForController(config),
                "PAGE_STATE_MIXIN_IMPORT" to FlutterProjectPathResolver.relativeImport(
                    config.targetPath,
                    config.pageStateMixinPath,
                ),
                "ENABLE_REFRESH" to config.enablePullToRefresh.toString(),
                "BUSINESS_STATE" to businessState,
                "LOAD_DATA_BODY" to loadDataBody,
                "REFRESH_BODY" to refreshBody,
                "PRIVATE_HELPERS" to privateHelpers,
            ),
        )
    }

    private fun repositoryExpression(config: RouteGenerateConfig): String {
        val repositoryClass = requireNotNull(config.repositoryClassName)
        return if (repositoryRequiresTkApi(config)) "$repositoryClass(TkApi())" else "$repositoryClass()"
    }

    private fun repositoryPath(config: RouteGenerateConfig): String? = when (config.repositoryMode) {
        RepositoryMode.CREATE -> "${config.libDirectory.path}/repositories/${config.repositoryFileName}"
        RepositoryMode.EXISTING -> requireNotNull(config.existingRepository).file.path
        RepositoryMode.NONE -> null
    }

    private fun repositoryRequiresTkApi(config: RouteGenerateConfig): Boolean = when (config.repositoryMode) {
        RepositoryMode.CREATE -> true
        RepositoryMode.EXISTING -> requireNotNull(config.existingRepository).unnamedConstructorParameters
            ?.singleOrNull()
            ?.typeText
            ?.substringAfterLast('.') == "TkApi"
        RepositoryMode.NONE -> false
    }

    private fun render(template: String, values: Map<String, String>): String = values.entries.fold(template) { text, entry ->
        text.replace("{{${entry.key}}}", entry.value)
    }.trimStart('\uFEFF').trimEnd() + "\n"
}
