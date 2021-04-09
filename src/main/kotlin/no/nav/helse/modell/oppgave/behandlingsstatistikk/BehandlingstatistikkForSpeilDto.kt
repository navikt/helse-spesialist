package no.nav.helse.modell.oppgave.behandlingsstatistikk

import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype

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

    internal companion object {
        internal fun toSpeilMap(behandlingsstatistikkDto: BehandlingsstatistikkDto) = BehandlingstatistikkForSpeilDto(
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

        private fun List<Pair<Saksbehandleroppgavetype, Int>>.toPerPeriodetypeForSpeil() =
            groupBy { toPeriodetypeForSpeil(it.first) }
            .map { (key, value) ->
                PerPeriodetype(key, value.sumBy { it.second })
            }

        private fun toPeriodetypeForSpeil(periodetype: Saksbehandleroppgavetype) = when (periodetype) {
            Saksbehandleroppgavetype.FORLENGELSE,
            Saksbehandleroppgavetype.INFOTRYGDFORLENGELSE -> PeriodetypeForSpeil.FORLENGELSE
            Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING -> PeriodetypeForSpeil.FØRSTEGANGSBEHANDLING
            Saksbehandleroppgavetype.OVERGANG_FRA_IT -> PeriodetypeForSpeil.OVERGANG_FRA_IT
        }
    }
}

