package com.frame.xplugins.getx.model

data class RouteBusinessMixinSpec(
    val mixinClassName: String,
    val controllerClassName: String,
    val fileSuffix: String = "_actions_mixin.dart",
    val templatePath: String = "mixin/actions_mixin.dart.tpl",
)

/** Builds route ActionMixin names from the generated page class, with no route-specific allowlist. */
object RouteBusinessMixinFactory {
    fun forPage(
        routeClassName: String,
        controllerClassName: String,
        pageType: PageType,
        controllerMode: ControllerMode,
    ): RouteBusinessMixinSpec? {
        if (pageType != PageType.STATEFUL || controllerMode == ControllerMode.NONE) return null

        return RouteBusinessMixinSpec(
            mixinClassName = "${routeClassName}ActionsMixin",
            controllerClassName = controllerClassName,
        )
    }
}
