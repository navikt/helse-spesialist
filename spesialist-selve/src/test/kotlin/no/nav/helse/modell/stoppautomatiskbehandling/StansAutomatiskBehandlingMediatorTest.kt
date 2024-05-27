package no.nav.helse.modell.stoppautomatiskbehandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now

class StansAutomatiskBehandlingMediatorTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)

    private val mediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            periodehistorikkDao,
            oppgaveDao,
            utbetalingDao,
            notatMediator,
        )

    @Test
    fun `Lagrer melding og periodehistorikk når opprettet er now`() {
        mediator.håndter(
            fødselsnummer = "12345678910",
            status = "STOPP_AUTOMATIKK",
            årsaker = setOf("MEDISINSK_VILKAR"),
            opprettet = now(),
            originalMelding = "{}",
            kilde = "ISYFO",
        )

        verify(exactly = 1) {
            stansAutomatiskBehandlingDao.lagre(
                fødselsnummer = "12345678910",
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf("MEDISINSK_VILKAR"),
                opprettet = any(),
                originalMelding = "{}",
                kilde = "ISYFO",
            )
        }
        verify(exactly = 1) {
            periodehistorikkDao.lagre(
                historikkType = STANS_AUTOMATISK_BEHANDLING,
                saksbehandlerOid = null,
                utbetalingId = any(),
                notatId = null,
            )
        }
    }

    @Test
    fun `Lagrer ikke periodehistorikk hvis opprettet er før dagens dato`() {
        mediator.håndter(
            fødselsnummer = "12345678910",
            status = "STOPP_AUTOMATIKK",
            årsaker = setOf("MEDISINSK_VILKAR"),
            opprettet = now().minusDays(1),
            originalMelding = "{}",
            kilde = "ISYFO",
        )

        verify(exactly = 1) {
            stansAutomatiskBehandlingDao.lagre(
                fødselsnummer = "12345678910",
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf("MEDISINSK_VILKAR"),
                opprettet = any(),
                originalMelding = "{}",
                kilde = "ISYFO",
            )
        }
        verify(exactly = 0) {
            periodehistorikkDao.lagre(
                historikkType = any(),
                saksbehandlerOid = any(),
                utbetalingId = any(),
                notatId = any(),
            )
        }
    }
}
