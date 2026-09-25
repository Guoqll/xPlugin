package com.frame.xplugins.getx.generator

import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.naming.DartNamingConverter
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GetXDataStateTemplateTest : BasePlatformTestCase() {

    fun testGeneratesMockDataStatePageAndController() {
        val config = dataStateConfig(generateMockExample = true, enablePullToRefresh = true)
        val generator = GetXCodeGenerator()

        val controller = generator.controller(config)
        val page = generator.page(config)
        val stateMixin = generator.pageStateMixin()
        val stateWidgets = generator.pageStateWidgets()

        assertTrue(controller.contains("with PageStateMixin"))
        assertTrue(controller.contains("bool get enableRefresh => true;"))
        assertTrue(controller.contains("final items = <String>[].obs;"))
        assertTrue(controller.contains("await Future.delayed(const Duration(milliseconds: 800));"))
        assertTrue(controller.contains("course_detail：\${_formatNow()}"))
        assertTrue(controller.contains("course_detail：\${_formatNow()}',\n    ];"))
        assertTrue(page.contains("PageRefreshView("))
        assertTrue(page.contains("PageStateView("))
        assertTrue(page.contains("controller.items.isEmpty"))
        assertTrue(page.contains("builder: _buildContent,\n          ),"))
        assertEquals(2, Regex("Obx\\(").findAll(page).count())
        assertTrue(stateMixin.contains("loading,"))
        assertTrue(stateMixin.contains("success,"))
        assertTrue(stateMixin.contains("error"))
        assertFalse(stateMixin.contains("empty"))
        assertFalse(stateWidgets.contains("isEmpty"))
        assertFalse(stateWidgets.contains("PageState<"))
    }

    fun testGeneratesTodoDataStateWithoutMock() {
        val config = dataStateConfig(generateMockExample = false, enablePullToRefresh = false)
        val generator = GetXCodeGenerator()

        val controller = generator.controller(config)
        val page = generator.page(config)

        assertTrue(controller.contains("bool get enableRefresh => false;"))
        assertTrue(controller.contains("TODO: 请求并处理页面数据"))
        assertTrue(controller.contains("TODO: 请求并处理刷新数据"))
        assertFalse(controller.contains("final items"))
        assertFalse(controller.contains("_mockRequest"))
        assertFalse(controller.contains("_formatNow"))
        assertTrue(page.contains("return const SizedBox();"))
        assertFalse(page.contains("controller.items"))
    }

    private fun dataStateConfig(
        generateMockExample: Boolean,
        enablePullToRefresh: Boolean,
    ): RouteGenerateConfig {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val parent = myFixture.tempDirFixture.findOrCreateDir("lib/features/course")
        val route = DartNamingConverter.routeName("course_detail")
        return RouteGenerateConfig(
            parentDirectory = parent,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.CREATE,
            existingBinding = null,
            createWidgetsDirectory = true,
            pageCapability = PageCapability.DATA_STATE,
            enablePullToRefresh = enablePullToRefresh,
            generateMockExample = generateMockExample,
        )
    }
}
