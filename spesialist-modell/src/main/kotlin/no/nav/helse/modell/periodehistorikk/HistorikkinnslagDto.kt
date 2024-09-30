package no.nav.helse.modell.periodehistorikk

import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import java.time.LocalDateTime

sealed interface HistorikkinnslagDto {
    val type: Innslagstype
    val notat: NotatDto?
    val saksbehandler: SaksbehandlerDto?

    companion object {
        fun lagtPåVentInnslag(
            notat: NotatDto,
            saksbehandler: SaksbehandlerDto,
        ): LagtPåVent {
            return LagtPåVent(
                type = Innslagstype.LAGT_PA_VENT,
                notat = notat,
                saksbehandler = saksbehandler,
                tidspunkt = LocalDateTime.now(),
            )
        }

        fun fjernetFraPåVentInnslag(saksbehandler: SaksbehandlerDto): FjernetFraPåVent {
            return FjernetFraPåVent(
                type = Innslagstype.FJERNET_FRA_PA_VENT,
                saksbehandler = saksbehandler,
            )
        }
    }
}

data class NotatDto(
    val oppgaveId: Long,
    val tekst: String,
)

data class LagtPåVent(
    override val type: Innslagstype,
    override val notat: NotatDto,
    override val saksbehandler: SaksbehandlerDto,
    val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto

data class FjernetFraPåVent(
    override val type: Innslagstype,
    override val saksbehandler: SaksbehandlerDto,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
}

enum class Innslagstype {
    LAGT_PA_VENT,
    FJERNET_FRA_PA_VENT,
}
