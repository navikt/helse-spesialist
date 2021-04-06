package no.nav.helse.modell.oppgave.behandlingsstatistikk

import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype


data class BehandlingstatistikkForSpeilDto(
    val antallOppgaverTilGodkjenning: AntallOppgaverTilGodkjenningForSpeilDto,
    val antallTildelteOppgaver: Int,
    val antallGodkjenteOppgaver: Int,
    val antallAnnulleringer: Int
) {
    data class AntallOppgaverTilGodkjenningForSpeilDto(
        val totalt: Int,
        val perPeriodetype: Map<PeriodetypeForSpeil, Int>
    )

    enum class PeriodetypeForSpeil {
        FØRSTEGANGSBEHANDLING,
        FORLENGELSE,
        INFOTRYGDFORLENGELSE,
        OVERGANG_FRA_IT
    }

    internal companion object {
        internal fun toSpeilMap(behandlingsstatistikkDto: BehandlingsstatistikkDto) = BehandlingstatistikkForSpeilDto(
            antallOppgaverTilGodkjenning = AntallOppgaverTilGodkjenningForSpeilDto(
                totalt = behandlingsstatistikkDto.oppgaverTilGodkjenning.totalt,
                perPeriodetype = behandlingsstatistikkDto.oppgaverTilGodkjenning.perPeriodetype.map { (type, antall) ->
                    toPeriodetypeForSpeil(type) to antall
                }.toMap()
            ),
            antallTildelteOppgaver = behandlingsstatistikkDto.antallTildelteOppgaver,
            antallGodkjenteOppgaver = behandlingsstatistikkDto.antallGodkjenteOppgaver,
            antallAnnulleringer = behandlingsstatistikkDto.antallAnnulleringer
        )

        private fun toPeriodetypeForSpeil(periodetype: Saksbehandleroppgavetype) = when (periodetype) {
            Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING -> PeriodetypeForSpeil.FØRSTEGANGSBEHANDLING
            Saksbehandleroppgavetype.FORLENGELSE -> PeriodetypeForSpeil.FORLENGELSE
            Saksbehandleroppgavetype.INFOTRYGDFORLENGELSE -> PeriodetypeForSpeil.INFOTRYGDFORLENGELSE
            Saksbehandleroppgavetype.OVERGANG_FRA_IT -> PeriodetypeForSpeil.OVERGANG_FRA_IT
        }
    }
}

