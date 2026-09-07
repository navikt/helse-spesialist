package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.Automatiseringsresultat
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.application.InMemoryAutomatiseringDao
import no.nav.helse.spesialist.application.InMemoryCommandContextDao
import no.nav.helse.spesialist.application.InMemoryMeldingDao
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.Vedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class VurderAutomatiskInnvilgelseTest : ApplicationTest() {
    private val hendelseId = UUID.randomUUID()
    private val periodetype = Periodetype.FORLENGELSE

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val automatiseringDao = sessionContext.automatiseringDao
    private val vedtakRepository = sessionContext.vedtakRepository
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
            utbetaling = Utbetaling(behandling1.utbetalingId!!.value, 0, 0, Utbetalingtype.UTBETALING),
            godkjenningsbehov =
                godkjenningsbehovData(
                    id = hendelseId,
                    fødselsnummer = person.id.value,
                    organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    utbetalingId = behandling1.utbetalingId!!.value,
                    periodetype = periodetype,
                    json = """{ "@event_name": "behov" }""",
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                ),
            oppgaveService = mockk(relaxed = true),
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
        val vedtak = vedtakRepository.finn(behandling1.spleisBehandlingId!!)
        assertIs<Vedtak.Automatisk>(vedtak)
        assertEquals(listOf(behandling1.utbetalingId!!.value), automatiseringDao.automatisert)
        assertTrue(automatiseringDao.manuellSaksbehandling.isEmpty())
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden kan ikke automatiseres`() {
        val problemer = listOf("Problem 1", "Problem 2")
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Automatiseringsresultat.KanIkkeAutomatiseres(
                problemer,
            )
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertNull(vedtakRepository.finn(behandling1.spleisBehandlingId!!))
        assertTrue(automatiseringDao.automatisert.isEmpty())
        assertEquals(
            listOf(
                InMemoryAutomatiseringDao.ManuellSaksbehandling(
                    problemer,
                    vedtaksperiode1.id.value,
                    hendelseId,
                    behandling1.utbetalingId!!.value,
                ),
            ),
            automatiseringDao.manuellSaksbehandling,
        )
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden er stikkprøve`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Automatiseringsresultat.Stikkprøve(
                "En årsak",
            )
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertNull(vedtakRepository.finn(behandling1.spleisBehandlingId!!))
        assertTrue(automatiseringDao.automatisert.isEmpty())
        assertEquals(listOf(behandling1.utbetalingId!!.value), automatiseringDao.stikkprøver)
    }

    @Test
    fun `Ferdigstiller kjede når perioden kan behandles automatisk`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        commandContext.utfør(commandContextDao, sessionContext, outbox, UUID.randomUUID(), command)
        assertEquals("Ferdig", observatør.gjeldendeTilstand)
    }

    @Test
    fun `prøver på nytt selv om det har vært forsøkt fattet vedtak før, så lenge spleis ikke har kvittert`() {
        vedtakRepository.lagre(Vedtak.automatisk(behandling1.spleisBehandlingId!!))
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        assertEquals(listOf(behandling1.utbetalingId!!.value), automatiseringDao.automatisert)
    }

    @Test
    fun `prøver ikke på nytt hvis spleis har kvittert ut tidligere svar`() {
        vedtakRepository.lagre(Vedtak.automatisk(behandling1.spleisBehandlingId!!).also { it.markerSomBehandletAvSpleis() })
        every { automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(commandContext, sessionContext, outbox))

        assertTrue(automatiseringDao.automatisert.isEmpty())
    }

    private val commandContextDao = InMemoryCommandContextDao(InMemoryMeldingDao())
}
