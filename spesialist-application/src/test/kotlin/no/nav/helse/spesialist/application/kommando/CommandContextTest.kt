package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.convertToUUID
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class CommandContextTest : ApplicationTest() {
    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()
            val hendelser = mutableListOf<UtgåendeHendelse>()
            val utgåendeTilstandEndringer = mutableListOf<KommandokjedeEndretEvent>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }

            override fun hendelse(hendelse: UtgåendeHendelse) {
                hendelser.add(hendelse)
            }

            override fun tilstandEndret(event: KommandokjedeEndretEvent) {
                this.utgåendeTilstandEndringer.add(event)
            }
        }

    private val commandContext: CommandContext = CommandContext(CONTEXT).also { it.nyObserver(observer) }

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
    }

    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)

    @Test
    fun `Tom command context`() {
        assertNull(commandContext.get<TestObject1>())
    }

    @Test
    fun `executer kommando uten tilstand`() {
        TestCommand().apply {
            assertTrue(commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this))
            assertTrue(executed)
            assertFalse(resumed)
            verify(exactly = 1) { commandContextDao.ferdig(this@apply.id, CONTEXT) }
            verify(exactly = 0) { commandContextDao.suspendert(any(), any(), hash().convertToUUID(), any()) }
        }
    }

    @Test
    fun `resumer kommando med tilstand`() {
        val commandContext = CommandContext(CONTEXT, listOf(1))
        TestCommand().apply {
            assertTrue(commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this))
            assertFalse(executed)
            assertTrue(resumed)
            verify(exactly = 1) { commandContextDao.ferdig(this@apply.id, CONTEXT) }
            verify(exactly = 0) { commandContextDao.suspendert(any(), any(), hash().convertToUUID(), any()) }
        }
    }

    @Test
    fun `suspenderer ved execute`() {
        TestCommand(executeAction = { false }).apply {
            assertFalse(commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this))
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.suspendert(this@apply.id, CONTEXT, hash().convertToUUID(), any()) }
        }
    }

    @Test
    fun `suspenderer ved resume`() {
        val sti = listOf(1)
        val commandContext = CommandContext(CONTEXT, sti)
        TestCommand(resumeAction = { false }).apply {
            assertFalse(commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this))
            verify(exactly = 0) { commandContextDao.ferdig(any(), any()) }
            verify(exactly = 1) { commandContextDao.suspendert(this@apply.id, CONTEXT, hash().convertToUUID(), sti) }
        }
    }

    @Test
    fun ferdigstiller() {
        TestCommand(executeAction = { this.ferdigstill(commandContext) }).apply {
            commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this)
            verify(exactly = 1) { commandContextDao.ferdig(any(), any()) }
        }
    }

    @Test
    fun `lager kommandokjede_ferdigstilt hendelse når kommandokjeden ferdigstilles`() {
        TestCommand(executeAction = { this.ferdigstill(commandContext) }).apply {
            commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this)
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
            commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this)
        }
        val result = observer.utgåendeTilstandEndringer
        assertTrue(result.isNotEmpty())
        assertTrue(result.first() is KommandokjedeEndretEvent.Suspendert)
    }

    @Test
    fun `lager kommandokjede_avbrutt hendelse når kommandokjeden avbrytes`() {
        every { commandContextDao.avbryt(any(), any()) } returns listOf(Pair(commandContext.id(), HENDELSE))
        TestCommand(executeAction = {
            false
        }).apply {
            commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this)
        }
        commandContext.avbrytAlleForPeriode(commandContextDao, UUID.randomUUID())
        val result = observer.utgåendeTilstandEndringer
        assertTrue(result.isNotEmpty())
        assertTrue(result.last() is KommandokjedeEndretEvent.Avbrutt)
    }

    @Test
    fun `ferdigstiller selv ved suspendering`() {
        val commandContext = CommandContext(CONTEXT)
        TestCommand(executeAction = {
            this.ferdigstill(commandContext)
            false
        }).apply {
            commandContext.utfør(commandContextDao, sessionContext, outbox, this.id, this)
            verify(exactly = 1) { commandContextDao.ferdig(any(), any()) }
        }
    }

    @Test
    fun `Henter ut første av en gitt type`() {
        val testObject1 = TestObject1()
        val testObject2 = TestObject1()
        commandContext.add(testObject1)
        commandContext.add(testObject2)
        assertEquals(testObject1, commandContext.get<TestObject1>())
    }

    @Test
    fun `Henter ut riktig type`() {
        val testObject1 = TestObject1()
        val testObject2 = TestObject2()
        commandContext.add(testObject1)
        commandContext.add(testObject2)
        assertEquals(testObject1, commandContext.get<TestObject1>())
        assertEquals(testObject2, commandContext.get<TestObject2>())
    }

    @Test
    fun `samler opp behov`() {
        commandContext.behov(Behov.Vergemål)
        commandContext.behov(Behov.Fullmakt)
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
        val hendelse =
            VedtaksperiodeGodkjentAutomatisk(
                fødselsnummer = lagFødselsnummer(),
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                periodetype = "FØRSTEGANGSBEHANDLING",
            )
        commandContext.hendelse(hendelse)
        assertEquals(listOf(hendelse), observer.hendelser)
    }

    private class TestObject1

    private class TestObject2

    private class TestCommand(
        private val executeAction: Command.() -> Boolean = { true },
        private val resumeAction: Command.() -> Boolean = { true },
    ) : Command {
        var executed = false
        var resumed = false

        val id: UUID = HENDELSE

        override fun execute(
            commandContext: CommandContext,
            sessionContext: SessionContext,
            outbox: Outbox,
        ): Boolean {
            executed = true
            return executeAction(this)
        }

        override fun resume(
            commandContext: CommandContext,
            sessionContext: SessionContext,
            outbox: Outbox,
        ): Boolean {
            resumed = true
            return resumeAction(this)
        }
    }
}
