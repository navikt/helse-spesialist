package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CommandContextTest {

    private lateinit var context: CommandContext

    @BeforeEach
    private fun setupEach() {
        context = CommandContext()
    }

    @Test
    fun `Tom command context`() {
        assertNull(context.get<TestObject1>())
    }

    @Test
    fun `Henter ut første av en gitt type`() {
        val testObject1 = TestObject1("object 1")
        val testObject2 = TestObject1("object 2")
        context.add(testObject1)
        context.add(testObject2)
        assertEquals(testObject1, context.get<TestObject1>())
    }

    @Test
    fun `Henter ut riktig type`() {
        val testObject1 = TestObject1("object 1 type 1")
        val testObject2 = TestObject2("object 1 type 2")
        context.add(testObject1)
        context.add(testObject2)
        assertEquals(testObject1, context.get<TestObject1>())
        assertEquals(testObject2, context.get<TestObject2>())
    }

    private class TestObject1(val data: String)
    private class TestObject2(val data: String)
}
