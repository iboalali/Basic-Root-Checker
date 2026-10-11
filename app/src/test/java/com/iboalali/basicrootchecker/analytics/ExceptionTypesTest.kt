package com.iboalali.basicrootchecker.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExceptionTypesTest {

    @Test
    fun `reports the class name and never the message`() {
        val types = exceptionTypes(IndexOutOfBoundsException("Index 7 out of bounds for length 3"))

        assertEquals("java.lang.IndexOutOfBoundsException", types)
        assertFalse(types.contains("Index 7"))
    }

    @Test
    fun `walks the cause chain outermost first`() {
        val throwable =
            RuntimeException("Unable to start activity", IllegalStateException("bad state"))

        assertEquals(
            "java.lang.RuntimeException < java.lang.IllegalStateException",
            exceptionTypes(throwable),
        )
    }

    @Test
    fun `stops at a bounded depth`() {
        var throwable: Throwable = IllegalStateException()
        repeat(10) { throwable = RuntimeException(throwable) }

        assertEquals(4, exceptionTypes(throwable).split(" < ").size)
    }
}
