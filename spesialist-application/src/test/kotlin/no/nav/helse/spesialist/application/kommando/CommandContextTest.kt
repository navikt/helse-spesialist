package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.CommandContextDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.convertToUUID
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.spesialist.application.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class CommandContextTest {
    private lateinit var context: CommandContext

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
    }

    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()
            val hendelser = mutableListOf<UtgåendeHendelse>()
            val utgåendeTilstandEndringer = mutableListOf<KommandokjedeEndretEvent>()

            override fun behov(behov: Behov, commandContextId: UUID) {
                this.behov.add(behov)
            }

            override fun hendelse(hendelse: UtgåendeHendelse) {
                hendelser.add(hendelse)
            }

            override fun tilstandEndret(event: KommandokjedeEndretEvent) {
                this.utgåendeTilstandEndringer.add(event)
            }
        }

    @BeforeEach
    fun setupEach() {
        context = CommandContext(CONTEXT)
        context.nyObserver(observer)
    }

    @Test
    fun `Tom command context`() {
        assertNull(context.get<TestObject1>())
    }

    @Test
    fun `executer kommando uten tilstand`() {
        TestCommand().apply {
            assertTrue(context.utfør(commandContextDao, this.id, this))
            assertTrue(executed)
            assertFalse(resumed)
            verify(exactly = 1) { commandContextDao.ferdig(this@apply.id, CONTEXT) }
            verify(exactly = 0) { commandContextDao.suspendert(any(), any(), hash().convertToUUID(), any()) }
        }
    }

    @Test
    fun `resumer kommando med tilstand`() {
        context = CommandContext(CONTEXT, listOf(1))
        TestCommand().apply {
            assertTrue(context.utfør(commandContextDao, this.id, this))
            assertFalse(executed)
            assertTrue(resumed)
            verify(exactly = 1) { commandContextDao.ferdig(this@apply.id, CONTEXT) }
            verify(exactly = 0) { commandContextDao.suspendert(any(), any(), hash().convertToUUID(), any()) }
        }
    }

    @Test
    fun `suspenderer ved execute`() {
        TestCommand(executeAction = { false }).apply {
            assertFalse(context.utfør(commandContextDao, this.id, this))
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.suspendert(this@apply.id, CONTEXT, hash().convertToUUID(), any()) }
        }
    }

    @Test
    fun `suspenderer ved resume`() {
        val sti = listOf(1)
        context = CommandContext(CONTEXT, sti)
        TestCommand(resumeAction = { false }).apply {
            assertFalse(context.utfør(commandContextDao, this.id, this))
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.suspendert(this@apply.id, CONTEXT, hash().convertToUUID(), sti) }
        }
    }

    @Test
    fun ferdigstiller() {
        TestCommand(executeAction = { this.ferdigstill(context) }).apply {
            context.utfør(commandContextDao, this.id, this)
            verify(exactly = 1) { commandContextDao.ferdig(any(), any()) }
        }
    }

    @Test
    fun `lager kommandokjede_ferdigstilt hendelse når kommandokjeden ferdigstilles`() {
        TestCommand(executeAction = { this.ferdigstill(context) }).apply {
            context.utfør(commandContextDao, this.id, this)
        }
        val result = observer.utgåendeTilstandEndringer
        assertTrue(result.isNotEmpty())
        assertTrue(result.first() is KommandokjedeEndretEvent.Ferdig)
    }

    @Test
    fun `lager kommandokjede_suspendert hendelse når kommandokjeden suspenderes`() {
        TestCommand(executeAction = {
            false
        }).apply {
            context.utfør(commandContextDao, this.id, this)
        }
        val result = observer.utgåendeTilstandEndringer
        assertTrue(result.isNotEmpty())
        assertTrue(result.first() is KommandokjedeEndretEvent.Suspendert)
    }

    @Test
    fun `lager kommandokjede_avbrutt hendelse når kommandokjeden avbrytes`() {
        every { commandContextDao.avbryt(any(), any()) } returns listOf(Pair(context.id(), HENDELSE))
        TestCommand(executeAction = {
            false
        }).apply {
            context.utfør(commandContextDao, this.id, this)
        }
        context.avbrytAlleForPeriode(commandContextDao, UUID.randomUUID())
        val result = observer.utgåendeTilstandEndringer
        assertTrue(result.isNotEmpty())
        assertTrue(result.last() is KommandokjedeEndretEvent.Avbrutt)
    }

    @Test
    fun `ferdigstiller selv ved suspendering`() {
        context = CommandContext(CONTEXT)
        TestCommand(executeAction = {
            this.ferdigstill(context)
            false
        }).apply {
            context.utfør(commandContextDao, this.id, this)
            verify(exactly = 1) { commandContextDao.ferdig(any(), any()) }
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
        context.behov(Behov.Vergemål)
        context.behov(Behov.Fullmakt)
        val result = observer.behov
        assertEquals(2, result.size)
        assertEquals(setOf(Behov.Vergemål, Behov.Fullmakt), result.toSet())
    }

    @Test
    fun `har ingen behov`() {
        val result = observer.behov
        assertTrue(result.isEmpty())
        assertEquals(emptyList<Behov>(), result)
    }

    @Test
    fun `holder på meldinger`() {
        val hendelse = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = lagFødselsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            periodetype = "FØRSTEGANGSBEHANDLING"
        )
        context.hendelse(hendelse)
        assertEquals(listOf(hendelse), observer.hendelser)
    }

    private class TestObject1

    private class TestObject2

    private class TestCommand(
        private val executeAction: Command.() -> Boolean = { true },
        private val resumeAction: Command.() -> Boolean = { true },
    ) : Command() {
        var executed = false
        var resumed = false

        val id: UUID = HENDELSE

        override fun execute(context: CommandContext): Boolean {
            executed = true
            return executeAction(this)
        }

        override fun resume(context: CommandContext): Boolean {
            resumed = true
            return resumeAction(this)
        }

    }
}
