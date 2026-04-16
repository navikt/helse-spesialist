package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.AutomatiseringDao
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
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.InMemoryCommandContextDao
import no.nav.helse.spesialist.application.InMemoryMeldingDao
import no.nav.helse.spesialist.application.InMemoryVedtakRepository
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class VurderAutomatiskInnvilgelseTest : ApplicationTest() {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val fødselsnummer = "12345678910"
        private const val orgnummer = "123456789"
        private val hendelseId = UUID.randomUUID()
        private val periodetype = Periodetype.FORLENGELSE
    }

    private val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val legacyBehandling =
        LegacyBehandling(
            id = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            skjæringstidspunkt = 1 jan 2018,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            spleisBehandlingId = spleisBehandlingId.value,
        )
    private val automatiseringDao = mockk<AutomatiseringDao>(relaxed = true)
    private val vedtakRepository = InMemoryVedtakRepository()
    private val observatør =
        object : CommandContextObserver {
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
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(this.observatør) }
    private val command =
        VurderAutomatiskInnvilgelse(
            automatisering,
            GodkjenningMediator(
                opptegnelseRepository = mockk(relaxed = true),
            ),
            utbetaling = Utbetaling(utbetalingId, 0, 0, Utbetalingtype.UTBETALING),
            sykefraværstilfelle =
                Sykefraværstilfelle(
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = 1 jan 2018,
                    gjeldendeBehandlinger = listOf(legacyBehandling),
                ),
            godkjenningsbehov =
                godkjenningsbehovData(
                    id = hendelseId,
                    organisasjonsnummer = orgnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingId = utbetalingId,
                    periodetype = periodetype,
                    json = """{ "@event_name": "behov" }""",
                    spleisBehandlingId = spleisBehandlingId.value,
                ),
            automatiseringDao = automatiseringDao,
            oppgaveService = mockk(relaxed = true),
            vedtakRepository = vedtakRepository,
        )

    @Test
    fun `kaller automatiser utfør og returnerer true`() {
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        verify(exactly = 1) {
            automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `publiserer godkjenningsmelding ved automatisert godkjenning`() {
        every {
            automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        val løsning =
            this
                .observatør
                .hendelser
                .filterIsInstance<Godkjenningsbehovløsning>()
                .singleOrNull()
        assertNotNull(løsning)
        assertEquals(true, løsning.automatiskBehandling)
    }

    @Test
    fun `automatiserer når resultat er at perioden kan automatiseres`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        val vedtak = vedtakRepository.finn(spleisBehandlingId)
        assertIs<Vedtak.Automatisk>(vedtak)
        verify(exactly = 1) { automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 0) { automatiseringDao.manuellSaksbehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden kan ikke automatiseres`() {
        val problemer = listOf("Problem 1", "Problem 2")
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Automatiseringsresultat.KanIkkeAutomatiseres(
                problemer,
            )
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertNull(vedtakRepository.finn(spleisBehandlingId))
        verify(exactly = 0) { automatiseringDao.automatisert(any(), any(), any()) }
        verify(exactly = 1) { automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden er stikkprøve`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Automatiseringsresultat.Stikkprøve(
                "En årsak",
            )
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertNull(vedtakRepository.finn(spleisBehandlingId))
        verify(exactly = 0) { automatiseringDao.automatisert(any(), any(), any()) }
        verify(exactly = 1) { automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `Ferdigstiller kjede når perioden kan behandles automatisk`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        commandContext.utfør(commandContextDao, sessionContext, outbox, UUID.randomUUID(), command)
        assertEquals("Ferdig", observatør.gjeldendeTilstand)
    }

    @Test
    fun `prøver på nytt selv om det har vært forsøkt fattet vedtak før, så lenge spleis ikke har kvittert`() {
        vedtakRepository.lagre(Vedtak.automatisk(spleisBehandlingId))
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        verify(exactly = 1) { automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `prøver ikke på nytt hvis spleis har kvittert ut tidligere svar`() {
        vedtakRepository.lagre(Vedtak.automatisk(spleisBehandlingId).also { it.markerSomBehandletAvSpleis() })
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        verify(exactly = 0) { automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    private val commandContextDao = InMemoryCommandContextDao(InMemoryMeldingDao())
}
