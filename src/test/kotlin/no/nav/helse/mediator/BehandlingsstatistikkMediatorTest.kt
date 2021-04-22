package no.nav.helse.mediator

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingsstatistikkDto
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingsstatistikkDto.BehandlingerDto
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingsstatistikkDto.OppgavestatistikkDto
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PerPeriodetype
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PeriodetypeForSpeil
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PeriodetypeForSpeil.*
import no.nav.helse.modell.vedtak.Periodetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class BehandlingsstatistikkMediatorTest {
    val behandlingsstatistikkDao = mockk<BehandlingsstatistikkDao>()
    val behandlingsstatistikkMediator = BehandlingsstatistikkMediator(behandlingsstatistikkDao)

    @BeforeEach
    private fun beforeEach() {
        every { behandlingsstatistikkDao.oppgavestatistikk(any()) } returns
            BehandlingsstatistikkDto(
                oppgaverTilGodkjenning = OppgavestatistikkDto(
                    totalt = 34,
                    perPeriodetype = listOf(
                        Periodetype.FØRSTEGANGSBEHANDLING to 10,
                        Periodetype.FORLENGELSE to 9,
                        Periodetype.INFOTRYGDFORLENGELSE to 8,
                        Periodetype.OVERGANG_FRA_IT to 7
                    )
                ),
                tildelteOppgaver = OppgavestatistikkDto(
                    totalt = 34,
                    perPeriodetype = listOf(
                        Periodetype.FØRSTEGANGSBEHANDLING to 10,
                        Periodetype.FORLENGELSE to 9,
                        Periodetype.INFOTRYGDFORLENGELSE to 8,
                        Periodetype.OVERGANG_FRA_IT to 7
                    )
                ),
                fullførteBehandlinger = BehandlingerDto(
                    totalt = 10,
                    annullert = 8,
                    manuelt = 1,
                    automatisk = 1
                )
            )
    }

    @Test
    fun `kan mappe fra internt til eksternt dto-format`() {
        val eksternDto = behandlingsstatistikkMediator.hentSaksbehandlingsstatistikk()
        assertEquals(34, eksternDto.antallOppgaverTilGodkjenning.totalt)
        assertEquals(10, eksternDto.antallOppgaverTilGodkjenning.perPeriodetype[FØRSTEGANGSBEHANDLING])
        assertEquals(17, eksternDto.antallOppgaverTilGodkjenning.perPeriodetype[FORLENGELSE])
        assertEquals(7, eksternDto.antallOppgaverTilGodkjenning.perPeriodetype[OVERGANG_FRA_IT])

        assertEquals(34, eksternDto.antallTildelteOppgaver.totalt)
        assertEquals(10, eksternDto.antallTildelteOppgaver.perPeriodetype[FØRSTEGANGSBEHANDLING])
        assertEquals(17, eksternDto.antallTildelteOppgaver.perPeriodetype[FORLENGELSE])
        assertEquals(7, eksternDto.antallTildelteOppgaver.perPeriodetype[OVERGANG_FRA_IT])

        assertEquals(10, eksternDto.fullførteBehandlinger.totalt)
        assertEquals(8, eksternDto.fullførteBehandlinger.annulleringer)
        assertEquals(1, eksternDto.fullførteBehandlinger.automatisk)
        assertEquals(1, eksternDto.fullførteBehandlinger.manuelt)
    }

    private operator fun List<PerPeriodetype>.get(type: PeriodetypeForSpeil) = this.first { it.periodetypeForSpeil == type }.antall
}
