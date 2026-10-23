package com.frame.xplugins.getx.writer

import com.frame.xplugins.getx.model.BindingMode
import com.frame.xplugins.getx.model.ControllerMode
import com.frame.xplugins.getx.model.PageType
import com.frame.xplugins.getx.model.PageCapability
import com.frame.xplugins.getx.model.RepositoryMode
import com.frame.xplugins.getx.model.RouteDiMode
import com.frame.xplugins.getx.model.RouteGenerateConfig
import com.frame.xplugins.getx.model.GetXGenerationException
import com.frame.xplugins.getx.naming.DartNamingConverter
import com.frame.xplugins.getx.scanner.GetXProjectScanner
import com.frame.xplugins.getx.service.GetXRouteExecutor
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GetXRouteWriterTest : BasePlatformTestCase() {

    fun testCreatesDynamicallyNamedActionsMixinAndKeepsPageControllerMixin() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val parent = myFixture.tempDirFixture.findOrCreateDir("lib/pages/courses")
        val config = courseDetailConfig(parent, lib)

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(parent.findChild("course_detail"))
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_course_detail_page.dart")))
        val actionsMixin = VfsUtilCore.loadText(
            requireNotNull(target.findChild("tk_course_detail_actions_mixin.dart")),
        )
        val controllerMixin = VfsUtilCore.loadText(
            requireNotNull(lib.findFileByRelativePath("core/mixins/page_controller_mixin.dart")),
        )

        assertTrue(page.contains("import 'tk_course_detail_actions_mixin.dart';"))
        assertTrue(page.contains("with PageControllerMixin<CourseDetailPage, CourseDetailController>"))
        assertTrue(page.contains("CourseDetailActionsMixin<CourseDetailPage>"))
        assertTrue(actionsMixin.contains("mixin CourseDetailActionsMixin<W extends StatefulWidget> on State<W>"))
        assertTrue(actionsMixin.contains("CourseDetailController get controller;"))
        assertTrue(actionsMixin.contains("TODO: Add this route's click handlers, navigation, and Controller actions."))
        assertTrue(controllerMixin.contains("C get controller => _controller;"))
        assertTrue(requireNotNull(GetXProjectScanner(project).scan(lib).pageControllerMixin).isCompatible)
    }

    fun testExistingActionsMixinFileIsNotOverwritten() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val parent = myFixture.tempDirFixture.findOrCreateDir("lib/pages/courses")
        val existing = myFixture.addFileToProject(
            "lib/pages/courses/course_detail/tk_course_detail_actions_mixin.dart",
            "// Keep the user's existing Mixin.\n",
        ).virtualFile

        try {
            GetXRouteExecutor(project).execute(courseDetailConfig(parent, lib))
            fail("Expected an existing business Mixin conflict")
        } catch (expected: GetXGenerationException) {
            assertTrue(expected.message.orEmpty().contains("tk_course_detail_actions_mixin.dart already exists"))
        }

        assertEquals("// Keep the user's existing Mixin.\n", VfsUtilCore.loadText(existing))
        assertNull(parent.findChild("course_detail")?.findChild("tk_course_detail_page.dart"))
    }

    fun testActionMixinControllerGetterUsesExistingControllerType() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val parent = myFixture.tempDirFixture.findOrCreateDir("lib/pages/courses")
        myFixture.addFileToProject(
            "lib/shared/course_controller.dart",
            "import 'package:get/get.dart';\nclass CourseController extends GetxController {}",
        )
        val candidate = GetXProjectScanner(project).scan(lib).controllers.single()
        val config = courseDetailConfig(parent, lib).copy(
            controllerMode = ControllerMode.EXISTING,
            existingController = candidate,
        )

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(parent.findChild("course_detail"))
        val actionsMixin = VfsUtilCore.loadText(
            requireNotNull(target.findChild("tk_course_detail_actions_mixin.dart")),
        )
        assertTrue(actionsMixin.contains("CourseController get controller;"))
        assertFalse(actionsMixin.contains("CourseDetailController get controller;"))
    }

    private fun courseDetailConfig(
        parent: com.intellij.openapi.vfs.VirtualFile,
        lib: com.intellij.openapi.vfs.VirtualFile,
    ): RouteGenerateConfig {
        val route = DartNamingConverter.routeName("course_detail")
        return RouteGenerateConfig(
            parentDirectory = parent,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course_detail",
            pageType = PageType.STATEFUL,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = false,
            pageCapability = PageCapability.BASIC,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )
    }

    fun testCreatesNavigatorDataStateMixinAndSupportsSingleUndo() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val report = myFixture.tempDirFixture.findOrCreateDir("lib/pages/report")
        myFixture.addFileToProject("lib/api/tk_api.dart", "class TkApi {}")
        val route = DartNamingConverter.routeName("course")
        val config = RouteGenerateConfig(
            parentDirectory = report,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.STATEFUL,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.CREATE,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = true,
            pageCapability = PageCapability.DATA_STATE,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(report.findChild("course"))
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_course_page.dart")))
        val mixin = VfsUtilCore.loadText(
            requireNotNull(lib.findFileByRelativePath("core/mixins/page_controller_mixin.dart")),
        )
        assertTrue(page.contains("class CoursePage extends StatefulWidget"))
        assertTrue(page.contains("with PageControllerMixin<CoursePage, CourseController>"))
        assertTrue(page.contains("CourseRepository(TkApi())"))
        assertFalse(page.contains("DependencyScope"))
        assertTrue(mixin.contains("late final C _controller;"))
        assertTrue(mixin.contains("C get controller => _controller;"))
        assertTrue(mixin.contains("String get controllerTag => _controllerTag;"))
        assertTrue(mixin.contains("_controllerTag = '\$C-\${UniqueKey()}';"))
        assertTrue(mixin.contains("_controller = Get.put<C>"))
        assertTrue(mixin.contains("Get.isRegistered<C>(tag: _controllerTag)"))
        assertTrue(mixin.contains("Get.delete<C>(tag: _controllerTag)"))
        assertNull(lib.findFileByRelativePath("bindings/tk_course_binding.dart"))
        assertNull(lib.findChild("bindings"))
        assertTrue(requireNotNull(GetXProjectScanner(project).scan(lib).pageControllerMixin).isCompatible)

        val undoManager = UndoManager.getInstance(project)
        assertTrue(undoManager.isUndoAvailable(null))
        undoManager.undo(null)
        assertNull(report.findChild("course"))
        assertNull(lib.findChild("core"))
        assertNull(lib.findChild("repositories"))
        assertNull(lib.findChild("bindings"))
    }

    fun testUnrecognizedPageControllerMixinStopsBeforeRouteCreation() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val report = myFixture.tempDirFixture.findOrCreateDir("lib/pages/report")
        myFixture.addFileToProject(
            "lib/core/mixins/page_controller_mixin.dart",
            "class UnrelatedMixin {}\n",
        )
        val route = DartNamingConverter.routeName("course")
        val config = RouteGenerateConfig(
            parentDirectory = report,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.STATEFUL,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = false,
            pageCapability = PageCapability.BASIC,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )

        try {
            GetXRouteExecutor(project).execute(config)
            fail("Expected an unsafe PageControllerMixin conflict")
        } catch (expected: GetXGenerationException) {
            assertTrue(expected.message.orEmpty().contains("cannot be safely identified"))
        }

        assertNull(report.findChild("course"))
    }

    fun testIncompatiblePageControllerMixinStopsBeforeRouteCreation() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val report = myFixture.tempDirFixture.findOrCreateDir("lib/pages/report")
        myFixture.addFileToProject(
            "lib/core/mixins/page_controller_mixin.dart",
            """
                mixin PageControllerMixin<W extends StatefulWidget, C extends GetxController>
                    on State<W> {
                  late final C controller;
                  C createController();
                }
            """.trimIndent(),
        )
        val route = DartNamingConverter.routeName("course")
        val config = RouteGenerateConfig(
            parentDirectory = report,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.STATEFUL,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = false,
            pageCapability = PageCapability.BASIC,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )

        try {
            GetXRouteExecutor(project).execute(config)
            fail("Expected an incompatible PageControllerMixin conflict")
        } catch (expected: GetXGenerationException) {
            assertTrue(expected.message.orEmpty().contains("PageControllerMixin:"))
            assertTrue(expected.message.orEmpty().contains("unique Controller tag"))
        }

        assertNull(report.findChild("course"))
    }

    fun testNavigatorPageSafelyConstructsExistingControllerAndRepository() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        myFixture.addFileToProject("lib/api/tk_api.dart", "class TkApi {}")
        myFixture.addFileToProject(
            "lib/repositories/tk_course_repository.dart",
            """
                import '../api/tk_api.dart';

                class CourseRepository {
                  CourseRepository(this.api);
                  final TkApi api;
                }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "lib/shared/course_controller.dart",
            """
                import 'package:get/get.dart';
                import '../repositories/tk_course_repository.dart';

                class CourseController extends GetxController {
                  CourseController(this.repository);
                  final CourseRepository repository;
                }
            """.trimIndent(),
        )
        val scan = GetXProjectScanner(project).scan(lib)
        val route = DartNamingConverter.routeName("course_detail")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.STATEFUL,
            controllerMode = ControllerMode.EXISTING,
            existingController = scan.controllers.single(),
            repositoryMode = RepositoryMode.EXISTING,
            existingRepository = scan.repositories.single(),
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = false,
            pageCapability = PageCapability.BASIC,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(features.findChild("course_detail"))
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_course_detail_page.dart")))
        assertTrue(page.contains("with PageControllerMixin<CourseDetailPage, CourseController>"))
        assertTrue(page.contains("CourseController("))
        assertTrue(page.contains("CourseRepository(TkApi()),"))
        assertTrue(page.contains("../../shared/course_controller.dart"))
        assertFalse(page.contains("DependencyScope"))
        assertNull(target.findChild("tk_course_detail_controller.dart"))
    }

    fun testCreatesDataStateRouteAndCommonInfrastructureWithSingleUndo() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        myFixture.addFileToProject("lib/api/tk_api.dart", "class TkApi {}")
        val route = DartNamingConverter.routeName("course_detail")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.CREATE,
            existingRepository = null,
            bindingMode = BindingMode.CREATE,
            existingBinding = null,
            createWidgetsDirectory = true,
            pageCapability = PageCapability.DATA_STATE,
            enablePullToRefresh = true,
            generateMockExample = true,
        )

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(features.findChild("course_detail"))
        val controller = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_course_detail_controller.dart")))
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_course_detail_page.dart")))
        val stateMixin = requireNotNull(lib.findFileByRelativePath("core/mixins/page_state_mixin.dart"))
        val stateWidgets = requireNotNull(
            lib.findFileByRelativePath("core/widgets/page_state/page_state_widgets.dart"),
        )
        val binding = VfsUtilCore.loadText(
            requireNotNull(lib.findFileByRelativePath("bindings/tk_course_binding.dart")),
        )
        assertTrue(controller.contains("with PageStateMixin"))
        assertTrue(controller.contains("course_detail：\${_formatNow()}"))
        assertTrue(page.contains("PageStateView("))
        assertTrue(page.contains("controller.items.isEmpty"))
        assertFalse(VfsUtilCore.loadText(stateMixin).contains("PageStatus.empty"))
        assertFalse(VfsUtilCore.loadText(stateWidgets).contains("isEmpty"))
        assertTrue(binding.contains("void _registerCourseRepository()"))
        assertTrue(binding.contains("_registerCourseRepository();"))
        assertFalse(binding.contains("void _registerRepository()"))

        val undoManager = UndoManager.getInstance(project)
        assertTrue(undoManager.isUndoAvailable(null))
        undoManager.undo(null)
        assertNull(features.findChild("course_detail"))
        assertNull(lib.findChild("core"))
        assertNull(lib.findChild("repositories"))
        assertNull(lib.findChild("bindings"))
    }

    fun testReusesCommonInfrastructureAndGeneratesTodoDataState() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        val mixin = myFixture.addFileToProject(
            "lib/core/mixins/page_state_mixin.dart",
            "// Keep custom page state mixin.\n",
        ).virtualFile
        val widgets = myFixture.addFileToProject(
            "lib/core/widgets/page_state/page_state_widgets.dart",
            "// Keep custom page state widgets.\n",
        ).virtualFile
        val mixinBefore = VfsUtilCore.loadText(mixin)
        val widgetsBefore = VfsUtilCore.loadText(widgets)
        val route = DartNamingConverter.routeName("report")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "report",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.CREATE,
            existingBinding = null,
            createWidgetsDirectory = false,
            pageCapability = PageCapability.DATA_STATE,
            enablePullToRefresh = false,
            generateMockExample = false,
        )

        GetXRouteExecutor(project).execute(config)

        assertEquals(mixinBefore, VfsUtilCore.loadText(mixin))
        assertEquals(widgetsBefore, VfsUtilCore.loadText(widgets))
        val target = requireNotNull(features.findChild("report"))
        val controller = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_report_controller.dart")))
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("tk_report_page.dart")))
        assertTrue(controller.contains("bool get enableRefresh => false;"))
        assertTrue(controller.contains("TODO: 请求并处理页面数据"))
        assertFalse(controller.contains("_mockRequest"))
        assertTrue(page.contains("return const SizedBox();"))
    }

    fun testCreatesCompleteRouteAndSupportsSingleUndo() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        myFixture.addFileToProject("lib/api/tk_api.dart", "class TkApi {}")
        val route = DartNamingConverter.routeName("course")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.CREATE,
            existingRepository = null,
            bindingMode = BindingMode.CREATE,
            existingBinding = null,
            createWidgetsDirectory = true,
        )

        GetXRouteExecutor(project).execute(config)

        val target = features.findChild("course")
        assertNotNull(target?.findChild("tk_course_page.dart"))
        assertNotNull(target?.findChild("tk_course_controller.dart"))
        assertNotNull(target?.findChild("widgets"))
        assertNotNull(lib.findFileByRelativePath("repositories/tk_course_repository.dart"))
        assertNotNull(lib.findFileByRelativePath("bindings/tk_course_binding.dart"))
        assertTrue(
            VfsUtilCore.loadText(requireNotNull(target?.findChild("tk_course_controller.dart")))
                .contains("../../repositories/tk_course_repository.dart"),
        )
        val page = VfsUtilCore.loadText(requireNotNull(target?.findChild("tk_course_page.dart")))
        val controller = VfsUtilCore.loadText(requireNotNull(target?.findChild("tk_course_controller.dart")))
        val repository = VfsUtilCore.loadText(
            requireNotNull(lib.findFileByRelativePath("repositories/tk_course_repository.dart")),
        )
        val binding = VfsUtilCore.loadText(requireNotNull(lib.findFileByRelativePath("bindings/tk_course_binding.dart")))
        assertTrue(page.contains("/// CoursePage 路由页面。"))
        assertTrue(page.contains("Scaffold(body: SizedBox())"))
        assertTrue(controller.contains("/// CourseController 负责当前路由的状态与业务逻辑。"))
        assertTrue(repository.contains("/// CourseRepository 负责当前业务域的数据访问。"))
        assertTrue(binding.contains("/// 注册并复用 [CourseRepository]，供当前 Binding 文件中的多个路由使用。"))
        assertTrue(binding.contains("/// [CoursePage] 的依赖绑定。"))
        assertTrue(binding.contains("void _registerCourseRepository()"))
        assertTrue(binding.contains("CourseRepository(TkApi())"))
        assertFalse(binding.contains("Get.find<TkApi>(),"))
        assertFalse(binding.contains("Get.find<CourseRepository>(),"))
        assertTrue(binding.contains("fenix: true,"))
        assertTrue(binding.contains("() => CourseController(Get.find<CourseRepository>()),"))

        val undoManager = UndoManager.getInstance(project)
        assertTrue("Generation should be one undoable command", undoManager.isUndoAvailable(null))
        undoManager.undo(null)
        assertNull(features.findChild("course"))
        assertNull(lib.findChild("repositories"))
        assertNull(lib.findChild("bindings"))
    }

    fun testAppendsBindingWithMinimalDiffAndDoesNotModifyRepository() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val course = myFixture.tempDirFixture.findOrCreateDir("lib/features/course")
        myFixture.addFileToProject("lib/api/tk_api.dart", "class TkApi {}")
        val repositoryFile = myFixture.addFileToProject(
            "lib/repositories/tk_course_repository.dart",
            """
                import '../api/tk_api.dart';

                class CourseRepository {
                  final TkApi _tkApi;

                  CourseRepository(this._tkApi);
                }
            """.trimIndent() + "\n",
        ).virtualFile
        val bindingFile = myFixture.addFileToProject(
            "lib/bindings/tk_course_binding.dart",
            """
                import 'package:get/get.dart';

                import '../api/tk_api.dart';
                import '../features/course/tk_course_controller.dart';
                import '../repositories/tk_course_repository.dart';

                void _registerRepository() {
                  if (Get.isRegistered<CourseRepository>()) {
                    return;
                  }

                  Get.lazyPut<CourseRepository>(
                    () => CourseRepository(
                      Get.find<TkApi>(),
                    ),
                    fenix: true,
                  );
                }

                class CourseBinding extends Bindings {
                  @override
                  void dependencies() {
                    _registerRepository();

                    Get.lazyPut<CourseController>(
                      () => CourseController(
                        Get.find<CourseRepository>(),
                      ),
                    );
                  }
                }
            """.trimIndent() + "\n",
        ).virtualFile
        val repositoryBefore = VfsUtilCore.loadText(repositoryFile)
        val bindingBefore = VfsUtilCore.loadText(bindingFile)
        val initialScan = GetXProjectScanner(project).scan(lib)
        val route = DartNamingConverter.routeName("course_detail")
        val config = RouteGenerateConfig(
            parentDirectory = course,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.EXISTING,
            existingRepository = initialScan.repositories.single(),
            bindingMode = BindingMode.APPEND,
            existingBinding = initialScan.bindings.single(),
            createWidgetsDirectory = true,
        )

        GetXRouteExecutor(project).execute(config)

        assertEquals(repositoryBefore, VfsUtilCore.loadText(repositoryFile))
        val bindingPsi = requireNotNull(PsiManager.getInstance(project).findFile(bindingFile))
        val bindingAfter = requireNotNull(PsiDocumentManager.getInstance(project).getDocument(bindingPsi)).text
        assertTrue(
            bindingAfter,
            bindingAfter.contains("import '../features/course/course_detail/tk_course_detail_controller.dart';"),
        )
        assertTrue(bindingAfter.contains("class CourseDetailBinding extends Bindings"))
        assertTrue(bindingAfter.contains("/// [CourseDetailPage] 的依赖绑定。"))
        val appendedBinding = bindingAfter.substring(bindingAfter.indexOf("/// [CourseDetailPage]"))
        assertFalse(appendedBinding.contains("Get.find<CourseRepository>(),"))
        assertTrue(appendedBinding.contains("() => CourseDetailController(Get.find<CourseRepository>()),"))
        assertEquals(1, Regex("void _registerRepository\\(\\)").findAll(bindingAfter).count())
        bindingBefore.lineSequence().filter(String::isNotBlank).forEach { originalLine ->
            assertTrue("Original line changed or removed: $originalLine", bindingAfter.contains(originalLine))
        }
    }

    fun testAppendsDedicatedRepositoryHelperBesideLegacyHelper() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val exam = myFixture.tempDirFixture.findOrCreateDir("lib/features/exam")
        myFixture.addFileToProject("lib/api/tk_api.dart", "class TkApi {}")
        myFixture.addFileToProject(
            "lib/repositories/tk_course_repository.dart",
            "class CourseRepository {}\n",
        )
        myFixture.addFileToProject(
            "lib/repositories/tk_exam_repository.dart",
            "class ExamRepository {}\n",
        )
        val bindingFile = myFixture.addFileToProject(
            "lib/bindings/tk_course_binding.dart",
            """
                import 'package:get/get.dart';

                import '../api/tk_api.dart';
                import '../repositories/tk_course_repository.dart';

                void _registerRepository() {
                  Get.lazyPut<CourseRepository>(CourseRepository.new);
                }

                class CourseBinding extends Bindings {
                  @override
                  void dependencies() {
                    _registerRepository();
                  }
                }
            """.trimIndent() + "\n",
        ).virtualFile
        val scan = GetXProjectScanner(project).scan(lib)
        val route = DartNamingConverter.routeName("exam_detail")
        val config = RouteGenerateConfig(
            parentDirectory = exam,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "exam",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.EXISTING,
            existingRepository = scan.repositories.single { it.className == "ExamRepository" },
            bindingMode = BindingMode.APPEND,
            existingBinding = scan.bindings.single(),
            createWidgetsDirectory = false,
        )

        GetXRouteExecutor(project).execute(config)

        val bindingPsi = requireNotNull(PsiManager.getInstance(project).findFile(bindingFile))
        val bindingAfter = requireNotNull(PsiDocumentManager.getInstance(project).getDocument(bindingPsi)).text
        assertTrue(bindingAfter.contains("void _registerRepository()"))
        assertTrue(bindingAfter.contains("void _registerExamRepository()"))
        assertTrue(bindingAfter.contains("Get.lazyPut<ExamRepository>"))
        val appendedBinding = bindingAfter.substring(bindingAfter.indexOf("/// [ExamDetailPage]"))
        assertTrue(appendedBinding.contains("_registerExamRepository();"))
        assertFalse(appendedBinding.contains("_registerRepository();"))
    }

    fun testCreatesSimplePageWithoutOptionalFiles() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        val route = DartNamingConverter.routeName("about")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "",
            businessDomain = "about",
            pageType = PageType.STATELESS,
            controllerMode = ControllerMode.NONE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = false,
        )

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(features.findChild("about"))
        assertEquals(listOf("about_page.dart"), target.children.map { it.name })
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("about_page.dart")))
        assertTrue(page.contains("class AboutPage extends StatelessWidget"))
        assertFalse(page.contains("package:get/get.dart"))
        assertNull(lib.findChild("repositories"))
        assertNull(lib.findChild("bindings"))
    }

    fun testCreatesNavigatorBasicPageWithoutControllerOrLifecycleMixin() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        val route = DartNamingConverter.routeName("about")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "",
            businessDomain = "about",
            pageType = PageType.STATELESS,
            controllerMode = ControllerMode.NONE,
            existingController = null,
            repositoryMode = RepositoryMode.NONE,
            existingRepository = null,
            bindingMode = BindingMode.NONE,
            existingBinding = null,
            createWidgetsDirectory = false,
            routeDiMode = RouteDiMode.NAVIGATOR_PAGE_SCOPE,
        )

        GetXRouteExecutor(project).execute(config)

        val target = requireNotNull(features.findChild("about"))
        val page = VfsUtilCore.loadText(requireNotNull(target.findChild("about_page.dart")))
        assertTrue(page.contains("class AboutPage extends StatelessWidget"))
        assertFalse(page.contains("PageControllerMixin"))
        assertNull(lib.findChild("core"))
        assertNull(lib.findChild("bindings"))
    }

    fun testConflictStopsBeforeAnyOtherFileIsCreated() {
        val lib = myFixture.tempDirFixture.findOrCreateDir("lib")
        val features = myFixture.tempDirFixture.findOrCreateDir("lib/features")
        val existing = myFixture.addFileToProject(
            "lib/features/course/tk_course_page.dart",
            "// keep me\n",
        ).virtualFile
        val route = DartNamingConverter.routeName("course")
        val config = RouteGenerateConfig(
            parentDirectory = features,
            libDirectory = lib,
            routeName = route.snakeCase,
            routeClassName = route.pascalCase,
            prefix = "tk",
            businessDomain = "course",
            pageType = PageType.GET_VIEW,
            controllerMode = ControllerMode.CREATE,
            existingController = null,
            repositoryMode = RepositoryMode.CREATE,
            existingRepository = null,
            bindingMode = BindingMode.CREATE,
            existingBinding = null,
            createWidgetsDirectory = true,
        )

        try {
            GetXRouteExecutor(project).execute(config)
            fail("Expected a file conflict")
        } catch (expected: GetXGenerationException) {
            assertTrue(expected.message.orEmpty().contains("tk_course_page.dart already exists"))
        }

        assertEquals("// keep me\n", VfsUtilCore.loadText(existing))
        assertNull(lib.findChild("repositories"))
        assertNull(lib.findChild("bindings"))
        assertNull(features.findFileByRelativePath("course/tk_course_controller.dart"))
    }
}
