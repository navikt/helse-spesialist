package no.nav.helse.behandlingsstatistikk

import no.nav.helse.vedtaksperiode.Periodetype

data class BehandlingstatistikkForSpeilDto(
    val antallOppgaverTilGodkjenning: OppgavestatistikkForSpeilDto,
    val antallTildelteOppgaver: OppgavestatistikkForSpeilDto,
    val fullførteBehandlinger: BehandlingerForSpeilDto
) {

    data class PerPeriodetype(
        val periodetypeForSpeil: PeriodetypeForSpeil,
        val antall: Int
    )

    data class OppgavestatistikkForSpeilDto(
        val totalt: Int,
        val perPeriodetype: List<PerPeriodetype>
    )

    data class BehandlingerForSpeilDto(
        val totalt: Int,
        val annulleringer: Int,
        val manuelt: Int,
        val automatisk: Int
    )

    enum class PeriodetypeForSpeil {
        FØRSTEGANGSBEHANDLING,
        FORLENGELSE,
        OVERGANG_FRA_IT
    }

    companion object {
        fun toSpeilMap(behandlingsstatistikkDto: BehandlingsstatistikkDto) = BehandlingstatistikkForSpeilDto(
            antallOppgaverTilGodkjenning = OppgavestatistikkForSpeilDto(
                totalt = behandlingsstatistikkDto.oppgaverTilGodkjenning.totalt,
                perPeriodetype = behandlingsstatistikkDto.oppgaverTilGodkjenning.perPeriodetype.toPerPeriodetypeForSpeil()
            ),
            antallTildelteOppgaver = OppgavestatistikkForSpeilDto(
                totalt = behandlingsstatistikkDto.tildelteOppgaver.totalt,
                perPeriodetype = behandlingsstatistikkDto.tildelteOppgaver.perPeriodetype.toPerPeriodetypeForSpeil()
            ),
            fullførteBehandlinger = BehandlingerForSpeilDto(
                totalt = behandlingsstatistikkDto.fullførteBehandlinger.totalt,
                annulleringer = behandlingsstatistikkDto.fullførteBehandlinger.annullert,
                manuelt = behandlingsstatistikkDto.fullførteBehandlinger.manuelt,
                automatisk = behandlingsstatistikkDto.fullførteBehandlinger.automatisk,
            )
        )

        private fun List<Pair<Periodetype, Int>>.toPerPeriodetypeForSpeil() =
            groupBy { toPeriodetypeForSpeil(it.first) }
            .map { (key, value) ->
                PerPeriodetype(key, value.sumBy { it.second })
            }

        private fun toPeriodetypeForSpeil(periodetype: Periodetype) = when (periodetype) {
            Periodetype.FORLENGELSE,
            Periodetype.INFOTRYGDFORLENGELSE -> PeriodetypeForSpeil.FORLENGELSE
            Periodetype.FØRSTEGANGSBEHANDLING -> PeriodetypeForSpeil.FØRSTEGANGSBEHANDLING
            Periodetype.OVERGANG_FRA_IT -> PeriodetypeForSpeil.OVERGANG_FRA_IT
        }
    }
}

