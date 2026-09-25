package com.frame.xplugins.getx.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GetXRouteActionRegistrationTest : BasePlatformTestCase() {

    fun testGetXRouteActionIsRegisteredInFlutterGroup() {
        val actionManager = ActionManager.getInstance()
        val action = actionManager.getAction("com.frame.xplugins.getx.route")
        assertNotNull("GetX Route action must be registered", action)

        val flutterGroup = actionManager.getAction("com.load.plugin.flutter") as? DefaultActionGroup
        assertNotNull("xView/flutter action group must be registered", flutterGroup)
        assertTrue(
            "GetX Route action must be a child of xView/flutter",
            flutterGroup!!.getChildren(null).any {
                actionManager.getId(it) == "com.frame.xplugins.getx.route"
            },
        )
    }

    fun testActionRemainsEnabledWhenNestedMenuUpdateHasNoDirectoryData() {
        val action = GetXRouteAction()
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .build()
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }
}
