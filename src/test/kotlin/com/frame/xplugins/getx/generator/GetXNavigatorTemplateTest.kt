package com.frame.xplugins.getx.generator

import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.naming.DartNamingConverter
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GetXNavigatorTemplateTest : BasePlatformTestCase() {

    fun testNavigatorDataStateUsesPageControllerMixinAndKeepsCompositionInPage() {
        val config = navigatorConfig(PageCapability.DATA_STATE, RepositoryMode.CREATE)
        val generator = GetXCodeGenerator()

        val page = generator.page(config)
        val mixin = generator.pageControllerMixin()

        assertTrue(page.contains("class CoursePage extends StatefulWidget"))
        assertTrue(page.contains("with PageControllerMixin<CoursePage, CourseController>"))
        assertTrue(page.contains("CourseController createController()"))
        assertTrue(page.contains("CourseRepository(TkApi()),"))
        assertTrue(page.contains("PageStateView("))
        assertFalse(page.contains("extends GetView"))
        assertFalse(page.contains("DependencyScope"))
        assertFalse(page.contains("Get.put<CourseRepository>"))
        assertFalse(page.contains("Get.find<TkApi>"))
        assertTrue(mixin.contains("late final C _controller;"))
        assertTrue(mixin.contains("C get controller => _controller;"))
        assertTrue(mixin.contains("String get controllerTag => _controllerTag;"))
        assertTrue(mixin.contains("_controllerTag = '\$C-\${UniqueKey()}';"))
        assertTrue(mixin.contains("_controller = Get.put<C>("))
        assertTrue(mixin.contains("Get.put<C>("))
        assertTrue(mixin.contains("Get.isRegistered<C>(tag: _controllerTag)"))
        assertTrue(mixin.contains("Get.delete<C>(tag: _controllerTag)"))
        assertTrue(mixin.contains("@mustCallSuper"))
    }

    fun testNavigatorBasicDoesNotGenerateDataStateUi() {
        val page = GetXCodeGenerator().page(navigatorConfig(PageCapability.BASIC, RepositoryMode.NONE))

        assertTrue(page.contains("extends StatefulWidget"))
        assertTrue(page.contains("with PageControllerMixin<CoursePage, CourseController>"))
        assertTrue(page.contains("return CourseController();"))
        assertFalse(page.contains("DependencyScope"))
        assertFalse(page.contains("PageStateView"))
        assertFalse(page.contains("PageRefreshView"))
        assertFalse(page.contains("Obx("))
    }

    private fun navigatorConfig(
        capability: PageCapability,
        repositoryMode: RepositoryMode,
    ): RouteGenerateConfig {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val parent = myFixture.tempDirFixture.findOrCreateDir("lib/pages/report")
        val route = DartNamingConverter.routeName("course")
        return RouteGenerateConfig(
            parentDirectory = parent,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.STATEFUL,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = repositoryMode,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = true,
            pageCapability = capability,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )
    }
}
