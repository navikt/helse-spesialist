package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.*
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
    fun `executer kommando uten tilstand`() {
        TestCommand().apply {
            context.run(this)
            assertTrue(executed)
            assertFalse(resumed)
        }
    }

    @Test
    fun `resumer kommando med tilstand`() {
        context.tilstand(listOf(1))
        TestCommand().apply {
            context.run(this)
            assertFalse(executed)
            assertTrue(resumed)
        }
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

    @Test
    fun `samler opp behov`() {
        val packet = mapOf(
            "fødselsnummer" to "123"
        )
        context.behov("type 1", mapOf("param 1" to 1))
        context.behov("type 2")
        val result = context.behov(packet)
        assertTrue(context.harBehov())
        assertTrue(result.containsKey("type 1"))
        assertFalse(result.containsKey("type 2"))
        assertTrue(result.containsKey("contextId"))
        assertEquals(listOf("type 1", "type 2"), result.getValue("behov") as List<*>)
        assertEquals(mapOf("param 1" to 1), result.getValue("type 1") as Map<*, *>)
        assertEquals("123", result.getValue("fødselsnummer"))
    }

    @Test
    fun `har ingen behov`() {
        val packet = mapOf(
            "fødselsnummer" to "123"
        )
        val result = context.behov(packet)
        assertFalse(context.harBehov())
        assertEquals(packet, result)
    }

    private class TestObject1(val data: String)
    private class TestObject2(val data: String)
    private class TestCommand : Command() {
        var executed = false
        var resumed = false
        var undo = false

        override fun execute(context: CommandContext): Boolean {
            executed = true
            return true
        }

        override fun resume(context: CommandContext): Boolean {
            resumed = true
            return true
        }

        override fun undo(context: CommandContext) {
            undo = true
        }
    }
}
