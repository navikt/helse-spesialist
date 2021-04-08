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
        INFOTRYGDFORLENGELSE,
        OVERGANG_FRA_IT
    }

    internal companion object {
        internal fun toSpeilMap(behandlingsstatistikkDto: BehandlingsstatistikkDto) = BehandlingstatistikkForSpeilDto(
            antallOppgaverTilGodkjenning = OppgavestatistikkForSpeilDto(
                totalt = behandlingsstatistikkDto.oppgaverTilGodkjenning.totalt,
                perPeriodetype = behandlingsstatistikkDto.oppgaverTilGodkjenning.perPeriodetype.map { (type, antall) ->
                    PerPeriodetype(toPeriodetypeForSpeil(type), antall)
                }
            ),
            antallTildelteOppgaver = OppgavestatistikkForSpeilDto(
                totalt = behandlingsstatistikkDto.tildelteOppgaver.totalt,
                perPeriodetype = behandlingsstatistikkDto.tildelteOppgaver.perPeriodetype.map { (type, antall) ->
                    PerPeriodetype(toPeriodetypeForSpeil(type), antall)
                }
            ),
            fullførteBehandlinger = BehandlingerForSpeilDto(
                totalt = behandlingsstatistikkDto.fullførteBehandlinger.totalt,
                annulleringer = behandlingsstatistikkDto.fullførteBehandlinger.annullert,
                manuelt = behandlingsstatistikkDto.fullførteBehandlinger.manuelt,
                automatisk = behandlingsstatistikkDto.fullførteBehandlinger.automatisk,
            )
        )

        private fun toPeriodetypeForSpeil(periodetype: Saksbehandleroppgavetype) = when (periodetype) {
            Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING -> PeriodetypeForSpeil.FØRSTEGANGSBEHANDLING
            Saksbehandleroppgavetype.FORLENGELSE -> PeriodetypeForSpeil.FORLENGELSE
            Saksbehandleroppgavetype.INFOTRYGDFORLENGELSE -> PeriodetypeForSpeil.INFOTRYGDFORLENGELSE
            Saksbehandleroppgavetype.OVERGANG_FRA_IT -> PeriodetypeForSpeil.OVERGANG_FRA_IT
        }
    }
}

