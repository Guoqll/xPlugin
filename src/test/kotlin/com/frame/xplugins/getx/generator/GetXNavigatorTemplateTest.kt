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
        assertTrue(page.contains("CourseActionsMixin<CoursePage>"))
    }

    fun testActionsMixinIsDerivedFromTheGeneratedPageClassAndController() {
        val config = navigatorConfig(
            PageCapability.BASIC,
            RepositoryMode.NONE,
        ).copy(
            routeName = "unmapped_route",
            routeClassName = "CourseDetail",
        )
        val generator = GetXCodeGenerator()

        val page = generator.page(config)
        val mixin = generator.businessMixin(config)

        assertEquals("tk_unmapped_route_actions_mixin.dart", config.businessMixinFileName)
        assertTrue(page.contains("import 'tk_unmapped_route_actions_mixin.dart';"))
        assertTrue(page.contains("with PageControllerMixin<CourseDetailPage, CourseDetailController>"))
        assertTrue(page.contains("CourseDetailActionsMixin<CourseDetailPage>"))
        assertTrue(mixin.contains("mixin CourseDetailActionsMixin<W extends StatefulWidget> on State<W>"))
        assertTrue(mixin.contains("CourseDetailController get controller;"))
        assertFalse(mixin.contains("GetxController get controller;"))
        assertTrue(mixin.contains("TODO: Add this route's click handlers, navigation, and Controller actions."))
        assertFalse(mixin.contains("AnswerReportOfShuaTi"))
    }

    fun testAnswerReportUsesTheSameDynamicActionsMixinTemplate() {
        val config = navigatorConfig(PageCapability.BASIC, RepositoryMode.NONE).copy(
            routeName = "answer_report_of_shua_ti",
            routeClassName = "AnswerReportOfShuaTi",
        )

        val mixin = GetXCodeGenerator().businessMixin(config)

        assertEquals("tk_answer_report_of_shua_ti_actions_mixin.dart", config.businessMixinFileName)
        assertTrue(mixin.contains("mixin AnswerReportOfShuaTiActionsMixin<W extends StatefulWidget> on State<W>"))
        assertTrue(mixin.contains("AnswerReportOfShuaTiController get controller;"))
        assertFalse(mixin.contains("onNextGroupTap"))
    }

    fun testStatefulPageWithoutControllerDoesNotGenerateActionsMixin() {
        val config = navigatorConfig(PageCapability.BASIC, RepositoryMode.NONE).copy(
            controllerMode = ControllerMode.NONE,
        )

        assertNull(config.businessMixinFileName)
        assertFalse(GetXCodeGenerator().page(config).contains("ActionsMixin"))
    }

    fun testActionsMixinWorksWithExistingGetXPageControllerMechanism() {
        val config = navigatorConfig(PageCapability.BASIC, RepositoryMode.NONE).copy(
            routeDiMode = RouteDiMode.GETX_ROUTE_BINDING,
            bindingMode = BindingMode.CREATE,
        )

        val generator = GetXCodeGenerator()
        val page = generator.page(config)
        val mixin = generator.businessMixin(config)

        assertTrue(page.contains("import 'tk_course_actions_mixin.dart';"))
        assertTrue(page.contains("with CourseActionsMixin<CoursePage>"))
        assertTrue(page.contains("CourseController get controller => Get.find<CourseController>();"))
        assertTrue(mixin.contains("CourseController get controller;"))
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
