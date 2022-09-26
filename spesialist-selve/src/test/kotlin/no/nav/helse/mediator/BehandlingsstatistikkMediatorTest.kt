package no.nav.helse.mediator

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDto
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDto.BehandlingerDto
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDto.OppgavestatistikkDto
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkType
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PerPeriodetype
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PeriodetypeForSpeil
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PeriodetypeForSpeil.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BehandlingsstatistikkMediatorTest {
    private val behandlingsstatistikkDao = mockk<BehandlingsstatistikkDao>()
    private val behandlingsstatistikkMediator = BehandlingsstatistikkMediator(behandlingsstatistikkDao)

    @BeforeEach
    fun beforeEach() {
        every { behandlingsstatistikkDao.oppgavestatistikk(any()) } returns
            BehandlingsstatistikkDto(
                oppgaverTilGodkjenning = OppgavestatistikkDto(
                    totalt = 34,
                    perPeriodetype = listOf(
                        BehandlingsstatistikkType.FØRSTEGANGSBEHANDLING to 10,
                        BehandlingsstatistikkType.FORLENGELSE to 9,
                        BehandlingsstatistikkType.INFOTRYGDFORLENGELSE to 8,
                        BehandlingsstatistikkType.OVERGANG_FRA_IT to 7,
                        BehandlingsstatistikkType.UTBETALING_TIL_SYKMELDT to 6,
                        BehandlingsstatistikkType.DELVIS_REFUSJON to 5
                    )
                ),
                tildelteOppgaver = OppgavestatistikkDto(
                    totalt = 34,
                    perPeriodetype = listOf(
                        BehandlingsstatistikkType.FØRSTEGANGSBEHANDLING to 10,
                        BehandlingsstatistikkType.FORLENGELSE to 9,
                        BehandlingsstatistikkType.INFOTRYGDFORLENGELSE to 8,
                        BehandlingsstatistikkType.OVERGANG_FRA_IT to 7,
                        BehandlingsstatistikkType.UTBETALING_TIL_SYKMELDT to 6,
                        BehandlingsstatistikkType.DELVIS_REFUSJON to 5
                    )
                ),
                fullførteBehandlinger = BehandlingerDto(
                    totalt = 10,
                    annullert = 8,
                    manuelt = OppgavestatistikkDto(
                        totalt = 1,
                        perPeriodetype = listOf(
                            BehandlingsstatistikkType.DELVIS_REFUSJON to 1
                        )
                    ),
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
        assertEquals(6, eksternDto.antallOppgaverTilGodkjenning.perPeriodetype[UTBETALING_TIL_SYKMELDT])
        assertEquals(5, eksternDto.antallOppgaverTilGodkjenning.perPeriodetype[DELVIS_REFUSJON])

        assertEquals(34, eksternDto.antallTildelteOppgaver.totalt)
        assertEquals(10, eksternDto.antallTildelteOppgaver.perPeriodetype[FØRSTEGANGSBEHANDLING])
        assertEquals(17, eksternDto.antallTildelteOppgaver.perPeriodetype[FORLENGELSE])
        assertEquals(7, eksternDto.antallTildelteOppgaver.perPeriodetype[OVERGANG_FRA_IT])
        assertEquals(6, eksternDto.antallTildelteOppgaver.perPeriodetype[UTBETALING_TIL_SYKMELDT])
        assertEquals(5, eksternDto.antallTildelteOppgaver.perPeriodetype[DELVIS_REFUSJON])

        assertEquals(10, eksternDto.fullførteBehandlinger.totalt)
        assertEquals(8, eksternDto.fullførteBehandlinger.annulleringer)
        assertEquals(1, eksternDto.fullførteBehandlinger.automatisk)
        assertEquals(1, eksternDto.fullførteBehandlinger.manuelt.totalt)
    }

    private operator fun List<PerPeriodetype>.get(type: PeriodetypeForSpeil) = this.first { it.periodetypeForSpeil == type }.antall
}
