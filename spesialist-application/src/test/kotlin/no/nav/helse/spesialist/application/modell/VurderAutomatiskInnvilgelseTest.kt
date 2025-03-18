package no.nav.helse.spesialist.application.modell

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.CommandContextDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.Automatiseringsresultat
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VurderAutomatiskInnvilgelseTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val fødselsnummer = "12345678910"
        private const val orgnummer = "123456789"
        private val hendelseId = UUID.randomUUID()
        private val periodetype = Periodetype.FORLENGELSE
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val legacyBehandling =
        LegacyBehandling(UUID.randomUUID(), vedtaksperiodeId, 1 jan 2018, 31 jan 2018, 1 jan 2018)
    private val automatiseringDao = mockk<AutomatiseringDao>(relaxed = true)
    private val command =
        VurderAutomatiskInnvilgelse(
            automatisering,
            GodkjenningMediator(
                opptegnelseDao = mockk(relaxed = true),
            ),
            utbetaling = Utbetaling(utbetalingId, 0, 0, Utbetalingtype.UTBETALING),
            sykefraværstilfelle = Sykefraværstilfelle(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1 jan 2018,
                gjeldendeBehandlinger = listOf(legacyBehandling),
            ),
            godkjenningsbehov = godkjenningsbehovData(
                id = hendelseId,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                periodetype = periodetype,
                json = """{ "@event_name": "behov" }"""
            ),
            automatiseringDao = automatiseringDao,
            oppgaveService = mockk(relaxed = true),
        )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(this.observatør)
    }

    @Test
    fun `kaller automatiser utfør og returnerer true`() {
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            automatisering.utfør(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `publiserer godkjenningsmelding ved automatisert godkjenning`() {
        every {
            automatisering.utfør(any(), any(), any(), any(), any(), any())
        } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(context))

        val løsning =
            this
                .observatør
                .hendelser
                .filterIsInstance<Godkjenningsbehovløsning>()
                .singleOrNull()
        assertNotNull(løsning)
        assertEquals(true, løsning?.automatiskBehandling)
    }

    @Test
    fun `automatiserer når resultat er at perioden kan automatiseres`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        assertTrue(command.execute(context))
        verify(exactly = 1) { automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 0) { automatiseringDao.manuellSaksbehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden kan ikke automatiseres`() {
        val problemer = listOf("Problem 1", "Problem 2")
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanIkkeAutomatiseres(
            problemer
        )
        assertTrue(command.execute(context))
        verify(exactly = 0) { automatiseringDao.automatisert(any(), any(), any()) }
        verify(exactly = 1) { automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden er stikkprøve`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.Stikkprøve(
            "En årsak"
        )
        assertTrue(command.execute(context))
        verify(exactly = 0) { automatiseringDao.automatisert(any(), any(), any()) }
        verify(exactly = 1) { automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `Ferdigstiller kjede når perioden kan behandles automatisk`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        context.utfør(commandContextDao, UUID.randomUUID(), command)
        assertEquals("Ferdig", observatør.gjeldendeTilstand)
    }

    @Test
    fun `Ferdigstiller kjede når perioden er spesialsak som kan behandles automatisk`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        context.utfør(commandContextDao, UUID.randomUUID(), command)
        assertEquals("Ferdig", observatør.gjeldendeTilstand)
    }

    private val commandContextDao = object : CommandContextDao {
        override fun nyContext(meldingId: UUID) = error("Not implemented in test")
        override fun opprett(hendelseId: UUID, contextId: UUID) {}
        override fun ferdig(hendelseId: UUID, contextId: UUID) {}
        override fun suspendert(hendelseId: UUID, contextId: UUID, hash: UUID, sti: List<Int>) {}
        override fun feil(hendelseId: UUID, contextId: UUID) {}
        override fun tidsbrukForContext(contextId: UUID) = error("Not implemented in test")
        override fun avbryt(vedtaksperiodeId: UUID, contextId: UUID) = error("Not implemented in test")
        override fun finnSuspendert(contextId: UUID) = error("Not implemented in test")
        override fun finnSuspendertEllerFeil(contextId: UUID) = error("Not implemented in test")
    }

    private val observatør = object : CommandContextObserver {
        val hendelser = mutableListOf<UtgåendeHendelse>()
        lateinit var gjeldendeTilstand: String
            private set

        override fun hendelse(hendelse: UtgåendeHendelse) {
            hendelser.add(hendelse)
        }

        override fun tilstandEndret(event: KommandokjedeEndretEvent) {
            gjeldendeTilstand = event::class.simpleName!!
        }
    }
}
