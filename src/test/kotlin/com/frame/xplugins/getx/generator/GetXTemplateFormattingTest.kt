package com.frame.xplugins.getx.generator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetXTemplateFormattingTest {

    private val templatePaths = listOf(
        "binding/register_repository.dart.tpl",
        "binding/with_repository.dart.tpl",
        "binding/without_repository.dart.tpl",
        "common/page_controller_mixin.dart.tpl",
        "common/page_state_mixin.dart.tpl",
        "common/page_state_widgets.dart.tpl",
        "controller/data_state_with_repository.dart.tpl",
        "controller/data_state_without_repository.dart.tpl",
        "controller/with_repository.dart.tpl",
        "controller/without_repository.dart.tpl",
        "page/data_state_get_view.dart.tpl",
        "page/get_view.dart.tpl",
        "mixin/actions_mixin.dart.tpl",
        "page/navigator_basic_with_controller.dart.tpl",
        "page/navigator_data_state.dart.tpl",
        "page/stateless.dart.tpl",
        "page/stateless_with_controller.dart.tpl",
        "page/stateful.dart.tpl",
        "page/stateful_with_controller.dart.tpl",
        "repository/repository.dart.tpl",
    )

    @Test
    fun `multiline Dart structures use structural trailing commas`() {
        val loader = GetXTemplateLoader()
        assertTrue(loader.load("common/page_controller_mixin.dart.tpl").contains("tag: _controllerTag,\n    );"))
        assertTrue(loader.load("page/navigator_data_state.dart.tpl").contains("builder: _buildContent,\n          ),"))
        assertTrue(loader.load("page/data_state_get_view.dart.tpl").contains("builder: _buildContent,\n          ),"))
        assertTrue(loader.load("binding/register_repository.dart.tpl").contains("fenix: true,\n  );"))
        assertFalse(loader.load("controller/with_repository.dart.tpl").contains("this._repository,"))
    }

    @Test
    fun `generated Dart templates document their primary declaration`() {
        val loader = GetXTemplateLoader()
        templatePaths.forEach { path ->
            assertTrue("$path must contain a Dart documentation comment", loader.load(path).contains("///"))
        }
    }
}
