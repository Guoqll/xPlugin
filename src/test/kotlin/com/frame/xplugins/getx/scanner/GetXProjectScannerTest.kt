package com.frame.xplugins.getx.scanner

import com.frame.xplugins.getx.model.RouteDiMode
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GetXProjectScannerTest : BasePlatformTestCase() {

    fun testScansDartClassesInsteadOfFileNames() {
        myFixture.addFileToProject(
            "lib/features/course/not_named_like_controller.dart",
            """
                import 'package:get/get.dart';
                import '../../repositories/tk_course_repository.dart';

                class CourseController extends GetxController {}
                class CourseDetailController extends GetxController {
                  CourseDetailController(this.repository);
                  final CourseRepository repository;
                }
                class LooksLikeAController {}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "lib/features/course/fake_controller.dart",
            "class NotAController {}",
        )
        myFixture.addFileToProject(
            "lib/repositories/tk_course_repository.dart",
            "class CourseRepository {}",
        )
        myFixture.addFileToProject(
            "lib/bindings/tk_course_binding.dart",
            """
                import 'package:get/get.dart';

                class CourseBinding extends Bindings {
                  @override
                  void dependencies() {}
                }
            """.trimIndent(),
        )

        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val result = GetXProjectScanner(project).scan(lib)

        assertEquals(listOf("CourseController", "CourseDetailController"), result.controllers.map { it.className })
        assertEquals(listOf("CourseRepository"), result.repositories.map { it.className })
        assertEquals(listOf("lib/bindings/tk_course_binding.dart"), result.bindings.map { it.relativePath })
        assertEquals(emptyList<String>(), result.controllers.first().unnamedConstructorParameters)
        assertEquals(
            listOf("CourseRepository"),
            result.controllers.last().unnamedConstructorParameters?.map { it.typeText },
        )
    }

    fun testDetectsNavigatorEvidenceAndCompatiblePageControllerMixin() {
        myFixture.addFileToProject(
            "example/lib/main.dart",
            "Widget build(BuildContext context) => MaterialApp(home: const SizedBox());",
        )
        myFixture.addFileToProject(
            "lib/utils/route_util.dart",
            "Navigator.of(context).push(CupertinoPageRoute(builder: (_) => page));",
        )
        myFixture.addFileToProject(
            "lib/core/mixins/page_controller_mixin.dart",
            """
                import 'package:flutter/widgets.dart';
                import 'package:get/get.dart';

                mixin PageControllerMixin<W extends StatefulWidget, C extends GetxController>
                    on State<W> {
                  late final String _controllerTag;
                  late final C _controller;

                  C get controller => _controller;

                  @protected
                  String get controllerTag => _controllerTag;

                  @protected
                  C createController();

                  @mustCallSuper
                  @override
                  void initState() {
                    super.initState();
                    _controllerTag = '${'$'}C-${'$'}{UniqueKey()}';
                    _controller = Get.put<C>(createController(), tag: _controllerTag);
                  }

                  @mustCallSuper
                  @override
                  void dispose() {
                    if (Get.isRegistered<C>(tag: _controllerTag)) {
                      Get.delete<C>(tag: _controllerTag);
                    }
                    super.dispose();
                  }
                }
            """.trimIndent(),
        )

        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val result = GetXProjectScanner(project).scan(lib)

        assertEquals(RouteDiMode.NAVIGATOR_PAGE_SCOPE, result.navigationEvidence.recommendedMode)
        assertTrue(result.navigationEvidence.hasMaterialApp)
        assertTrue(result.navigationEvidence.hasNavigatorRoutes)
        val mixin = requireNotNull(result.pageControllerMixin)
        assertEquals("lib/core/mixins/page_controller_mixin.dart", mixin.relativePath)
        assertTrue(mixin.compatibilityErrors.joinToString(), mixin.isCompatible)
    }

    fun testKeepsLegacyV22PageControllerMixinCompatible() {
        myFixture.addFileToProject(
            "lib/core/mixins/page_controller_mixin.dart",
            """
                import 'package:flutter/widgets.dart';
                import 'package:get/get.dart';

                mixin PageControllerMixin<W extends StatefulWidget, C extends GetxController>
                    on State<W> {
                  late final String _controllerTag;
                  late final C controller;

                  C createController();

                  @mustCallSuper
                  @override
                  void initState() {
                    super.initState();
                    _controllerTag = UniqueKey().toString();
                    controller = Get.put<C>(createController(), tag: _controllerTag);
                  }

                  @mustCallSuper
                  @override
                  void dispose() {
                    Get.delete<C>(tag: _controllerTag);
                    super.dispose();
                  }
                }
            """.trimIndent(),
        )

        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val mixin = requireNotNull(GetXProjectScanner(project).scan(lib).pageControllerMixin)

        assertTrue(mixin.compatibilityErrors.joinToString(), mixin.isCompatible)
    }

    fun testRejectsPartiallyHardenedMixinWithoutProtectedTagOrDeleteGuard() {
        myFixture.addFileToProject(
            "lib/core/mixins/page_controller_mixin.dart",
            """
                import 'package:flutter/widgets.dart';
                import 'package:get/get.dart';

                mixin PageControllerMixin<W extends StatefulWidget, C extends GetxController>
                    on State<W> {
                  late final String _controllerTag;
                  late final C _controller;

                  C get controller => _controller;
                  C createController();

                  @mustCallSuper
                  @override
                  void initState() {
                    super.initState();
                    _controllerTag = '${'$'}C-${'$'}{UniqueKey()}';
                    _controller = Get.put<C>(createController(), tag: _controllerTag);
                  }

                  @mustCallSuper
                  @override
                  void dispose() {
                    Get.delete<C>(tag: _controllerTag);
                    super.dispose();
                  }
                }
            """.trimIndent(),
        )

        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val mixin = requireNotNull(GetXProjectScanner(project).scan(lib).pageControllerMixin)

        assertFalse(mixin.isCompatible)
        assertTrue(mixin.compatibilityErrors.any { "protected getter" in it })
        assertTrue(mixin.compatibilityErrors.any { "Get.isRegistered" in it })
    }
}
