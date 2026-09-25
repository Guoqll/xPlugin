package com.frame.xplugins.getx.naming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DartNamingConverterTest {

    @Test
    fun `normalizes supported route name styles`() {
        listOf("course_detail", "CourseDetail", "course-detail", "course detail").forEach { input ->
            assertEquals(DartName("course_detail", "CourseDetail"), DartNamingConverter.routeName(input))
        }
    }

    @Test
    fun `preserves acronym boundaries`() {
        assertEquals(DartName("http_client", "HttpClient"), DartNamingConverter.routeName("HTTPClient"))
        assertEquals(DartName("course_url_page", "CourseUrlPage"), DartNamingConverter.routeName("CourseURLPage"))
    }

    @Test
    fun `normalizes prefix underscores`() {
        listOf("tk", "tk_", "_tk", "_tk_").forEach { input ->
            assertEquals("tk", DartNamingConverter.prefix(input))
        }
        assertEquals("", DartNamingConverter.prefix("___"))
    }

    @Test
    fun `builds prefixed file stem without double underscore`() {
        assertEquals("tk_course", DartNamingConverter.prefixedFileStem("_tk_", "course"))
        assertEquals("course", DartNamingConverter.prefixedFileStem("", "course"))
    }

    @Test
    fun `rejects path and invalid identifier input`() {
        assertThrows(IllegalArgumentException::class.java) { DartNamingConverter.routeName("../course") }
        assertThrows(IllegalArgumentException::class.java) { DartNamingConverter.routeName("123course") }
        assertThrows(IllegalArgumentException::class.java) { DartNamingConverter.routeName("课程") }
    }
}
