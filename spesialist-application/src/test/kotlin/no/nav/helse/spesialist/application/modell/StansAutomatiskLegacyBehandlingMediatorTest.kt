package no.nav.helse.spesialist.application.modell

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStanset
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.spesialist.application.VeilederStansRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VeilederStans
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID

class StansAutomatiskLegacyBehandlingMediatorTest {
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val veilederStansRepository = mockk<VeilederStansRepository>(relaxed = true)

    private companion object {
        private const val FNR = "12345678910"
    }

    private val mediator =
        StansAutomatiskBehandlingMediator(
            periodehistorikkDao,
            oppgaveDao,
            veilederStansRepository,
        )

    @Test
    fun `Lagrer periodehistorikk når stoppknapp-melding håndteres`() {
        val melding =
            StansAutomatiskBehandlingMelding(
                id = randomUUID(),
                fødselsnummer = FNR,
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf(MEDISINSK_VILKAR),
                opprettet = now(),
                originalMelding = """{"uuid": "${randomUUID()}"}""",
                kilde = "ISYFO",
                json = "",
            )

        mediator.håndter(melding)

        verify(exactly = 1) {
            periodehistorikkDao.lagreMedOppgaveId(
                historikkinnslag = any<AutomatiskBehandlingStanset>(),
                oppgaveId = any(),
            )
        }
    }

    @Test
    fun `håndter STOPP_AUTOMATIKK oppretter og lagrer ny VeilederStans`() {
        val originalMeldingId = randomUUID()
        val melding =
            StansAutomatiskBehandlingMelding(
                id = randomUUID(),
                fødselsnummer = FNR,
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf(MEDISINSK_VILKAR, AKTIVITETSKRAV),
                opprettet = now(),
                originalMelding = """{"uuid": "$originalMeldingId"}""",
                kilde = "ISYFO",
                json = "",
            )
        val slot = slot<VeilederStans>()
        every { veilederStansRepository.lagre(capture(slot)) } just runs

        mediator.håndter(melding)

        verify(exactly = 1) { veilederStansRepository.lagre(any()) }
        val lagretStans = slot.captured
        assertEquals(Identitetsnummer.fraString(FNR), lagretStans.identitetsnummer)
        assertEquals(
            setOf(VeilederStans.StansÅrsak.MEDISINSK_VILKAR, VeilederStans.StansÅrsak.AKTIVITETSKRAV),
            lagretStans.årsaker,
        )
        assertEquals(originalMeldingId, lagretStans.originalMeldingId)
        assertTrue(lagretStans.erStansett)
    }

    @Test
    fun `håndter NORMAL lagrer ikke VeilederStans`() {
        val melding =
            StansAutomatiskBehandlingMelding(
                id = randomUUID(),
                fødselsnummer = FNR,
                status = "NORMAL",
                årsaker = emptySet(),
                opprettet = now(),
                originalMelding = "{}",
                kilde = "ISYFO",
                json = "",
            )

        mediator.håndter(melding)

        verify(exactly = 0) { veilederStansRepository.lagre(any()) }
    }
}
