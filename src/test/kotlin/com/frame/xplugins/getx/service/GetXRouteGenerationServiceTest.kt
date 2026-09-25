package com.frame.xplugins.getx.service

import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.naming.DartNamingConverter
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GetXRouteGenerationServiceTest : BasePlatformTestCase() {

    fun testBasicPreviewDoesNotShowCommonInfrastructureSections() {
        val preview = GetXRouteGenerationService().plan(config(PageCapability.BASIC)).render()

        assertFalse(preview.contains("COMMON INFRASTRUCTURE"))
    }

    fun testDataStatePreviewSeparatesCreatedAndReusedCommonInfrastructure() {
        myFixture.addFileToProject(
            "lib/core/mixins/page_state_mixin.dart",
            "// Existing project implementation.\n",
        )

        val preview = GetXRouteGenerationService().plan(config(PageCapability.DATA_STATE)).render()

        assertTrue(preview.contains("CREATE COMMON INFRASTRUCTURE"))
        assertTrue(preview.contains("lib/core/widgets/page_state/page_state_widgets.dart"))
        assertTrue(preview.contains("REUSE COMMON INFRASTRUCTURE"))
        assertTrue(preview.contains("lib/core/mixins/page_state_mixin.dart"))
    }

    fun testNavigatorPreviewShowsPageControllerMixinWithoutScopeOrBinding() {
        val preview = GetXRouteGenerationService().plan(
            config(PageCapability.BASIC, RouteDiMode.NAVIGATOR_PAGE_SCOPE),
        ).render()

        assertTrue(preview.contains("ROUTE / DI MODE\n  Navigator + Page Scope"))
        assertTrue(preview.contains("CREATE COMMON INFRASTRUCTURE"))
        assertTrue(preview.contains("lib/core/mixins/page_controller_mixin.dart"))
        assertFalse(preview.contains("DependencyScope"))
        assertFalse(preview.contains("_scope.dart"))
        assertFalse(preview.contains("tk_course_detail_binding.dart"))
    }

    private fun config(
        capability: PageCapability,
        routeDiMode: RouteDiMode = RouteDiMode.GETX_ROUTE_BINDING,
    ): RouteGenerateConfig {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val parent = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        val route = DartNamingConverter.routeName("course_detail")
        return RouteGenerateConfig(
            parentDirectory = parent,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course_detail",
            pageType = if (routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE) PageType.STATEFUL else PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = if (routeDiMode == RouteDiMode.NAVIGATOR_PAGE_SCOPE) BindingMode.NONE else BindingMode.CREATE,
            existingBinding = null,
            createWidgetsDirectory = true,
            pageCapability = capability,
            routeDiMode = routeDiMode,
        )
    }
}
