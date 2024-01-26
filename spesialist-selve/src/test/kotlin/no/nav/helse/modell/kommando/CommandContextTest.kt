package no.nav.helse.modell.kommando

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CommandContextTest {

    private lateinit var context: CommandContext

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "fnr"
        private const val SNAPSHOT = "json"
    }

    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)

    @BeforeEach
    fun setupEach() {
        context = CommandContext(CONTEXT)
    }

    @Test
    fun `Tom command context`() {
        assertNull(context.get<TestObject1>())
    }

    @Test
    fun `executer kommando uten tilstand`() {
        TestCommand().apply {
            assertTrue(context.utfør(commandContextDao, this))
            assertTrue(executed)
            assertFalse(resumed)
            verify(exactly = 1) { commandContextDao.ferdig(this@apply, CONTEXT) }
            verify(exactly = 0) { commandContextDao.suspendert(any(), any(), any()) }
        }
    }

    @Test
    fun `resumer kommando med tilstand`() {
        context = CommandContext(CONTEXT, listOf(1))
        TestCommand().apply {
            assertTrue(context.utfør(commandContextDao, this))
            assertFalse(executed)
            assertTrue(resumed)
            verify(exactly = 1) { commandContextDao.ferdig(this@apply, CONTEXT) }
            verify(exactly = 0) { commandContextDao.suspendert(any(), any(), any()) }
        }
    }

    @Test
    fun `suspenderer ved execute`() {
        context = CommandContext(CONTEXT)
        TestCommand(executeAction = { false }).apply {
            assertFalse(context.utfør(commandContextDao, this))
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.suspendert(this@apply, CONTEXT, any()) }
        }
    }

    @Test
    fun `suspenderer ved resume`() {
        val sti = listOf(1)
        context = CommandContext(CONTEXT, sti)
        TestCommand(resumeAction = { false }).apply {
            assertFalse(context.utfør(commandContextDao, this))
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.suspendert(this@apply, CONTEXT, sti) }
        }
    }

    @Test
    fun `feil ved execute`() {
        context = CommandContext(CONTEXT)
        TestCommand(executeAction = { throw Exception() }).apply {
            assertThrows<Exception> { context.utfør(commandContextDao, this) }
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.feil(this@apply, CONTEXT) }
        }
    }

    @Test
    fun `feil ved resume`() {
        val sti = listOf(1)
        context = CommandContext(CONTEXT, sti)
        TestCommand(resumeAction = { throw Exception() }).apply {
            assertThrows<Exception> { context.utfør(commandContextDao, this) }
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.feil(this@apply, CONTEXT) }
        }
    }

    @Test
    fun ferdigstiller() {
        context = CommandContext(CONTEXT)
        TestCommand(executeAction = { this.ferdigstill(context)}).apply {
            context.utfør(commandContextDao, this)
            verify(exactly = 1) { commandContextDao.ferdig(any(), any())}
        }
    }

    @Test
    fun `ferdigstiller selv ved suspendering`() {
        context = CommandContext(CONTEXT)
        TestCommand(executeAction = {
            this.ferdigstill(context)
            false
        }).apply {
            context.utfør(commandContextDao, this)
            verify(exactly = 1) { commandContextDao.ferdig(any(), any())}
        }
    }

    @Test
    fun `Henter ut første av en gitt type`() {
        val testObject1 = TestObject1()
        val testObject2 = TestObject1()
        context.add(testObject1)
        context.add(testObject2)
        assertEquals(testObject1, context.get<TestObject1>())
    }

    @Test
    fun `Henter ut riktig type`() {
        val testObject1 = TestObject1()
        val testObject2 = TestObject2()
        context.add(testObject1)
        context.add(testObject2)
        assertEquals(testObject1, context.get<TestObject1>())
        assertEquals(testObject2, context.get<TestObject2>())
    }

    @Test
    fun `samler opp behov`() {
        context.behov("type 1", mapOf("param 1" to 1))
        context.behov("type 2")
        val result = context.behov()
        assertTrue(context.harBehov())
        assertTrue(result.containsKey("type 1"))
        assertTrue(result.containsKey("type 2"))
        assertEquals(mapOf("param 1" to 1), result.getValue("type 1") as Map<*, *>)
    }

    @Test
    fun `har ingen behov`() {
        val result = context.behov()
        assertFalse(context.harBehov())
        assertEquals(emptyMap<String, Any>(), result)
    }

    @Test
    fun `holder på meldinger`() {
        val melding = """{ "a_key": "with_a_value" }"""
        context.publiser(melding)
        assertEquals(listOf(melding), context.meldinger())
    }

    @Test
    fun `overskriver behov som allerede finnes`() {
        context.behov("type 1", mapOf("param 1" to 1))
        context.behov("type 2", mapOf("param 2" to 1))
        assertEquals(mapOf(
            "type 1" to mapOf("param 1" to 1),
            "type 2" to mapOf("param 2" to 1)
        ), context.behov())
        context.behov("type 1", mapOf("param 1" to 2))
        assertEquals(mapOf(
            "type 1" to mapOf("param 1" to 2),
            "type 2" to mapOf("param 2" to 1)
        ), context.behov())
    }

    private class TestObject1
    private class TestObject2
    private class TestCommand(
        private val executeAction: Command.() -> Boolean = { true },
        private val resumeAction: Command.() -> Boolean = { true }
    ) : Kommandohendelse {
        var executed = false
        var resumed = false
        var undo = false

        override val id = HENDELSE
        override fun fødselsnummer() = FNR
        override fun vedtaksperiodeId() = VEDTAKSPERIODE
        override fun toJson() = SNAPSHOT

        override fun execute(context: CommandContext): Boolean {
            executed = true
            return executeAction(this)
        }

        override fun resume(context: CommandContext): Boolean {
            resumed = true
            return resumeAction(this)
        }

        override fun undo(context: CommandContext) {
            undo = true
        }
    }
}
