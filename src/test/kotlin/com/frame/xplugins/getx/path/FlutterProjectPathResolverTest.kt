package com.frame.xplugins.getx.path

import org.junit.Assert.assertEquals
import org.junit.Test

class FlutterProjectPathResolverTest {

    @Test
    fun `calculates dart relative imports`() {
        assertEquals(
            "../../repositories/tk_course_repository.dart",
            FlutterProjectPathResolver.relativeImport(
                "/project/lib/features/course",
                "/project/lib/repositories/tk_course_repository.dart",
            ),
        )
        assertEquals(
            "tk_course_controller.dart",
            FlutterProjectPathResolver.relativeImport(
                "/project/lib/features/course",
                "/project/lib/features/course/tk_course_controller.dart",
            ),
        )
    }
}
