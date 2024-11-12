package no.nav.helse.modell.periodehistorikk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface HistorikkinnslagDto {
    val notat: NotatDto?
    val dialogRef: Long?
    val saksbehandler: SaksbehandlerDto?
    val tidspunkt: LocalDateTime

    fun toJson(): String = "{}"

    companion object {
        fun lagtPåVentInnslag(
            notattekst: String?,
            saksbehandler: SaksbehandlerDto,
            årsaker: List<PåVentÅrsak>,
            frist: LocalDate,
            dialogRef: Long,
        ): LagtPåVent =
            LagtPåVent(
                notattekst = notattekst,
                saksbehandler = saksbehandler,
                årsaker = årsaker,
                tidspunkt = LocalDateTime.now(),
                frist = frist,
                dialogRef = dialogRef,
            )

        fun fjernetFraPåVentInnslag(saksbehandler: SaksbehandlerDto): FjernetFraPåVent =
            FjernetFraPåVent(
                saksbehandler = saksbehandler,
                tidspunkt = LocalDateTime.now(),
            )

        fun totrinnsvurderingFerdigbehandletInnslag(saksbehandler: SaksbehandlerDto): TotrinnsvurderingFerdigbehandlet =
            TotrinnsvurderingFerdigbehandlet(saksbehandler = saksbehandler, tidspunkt = LocalDateTime.now())

        fun avventerTotrinnsvurdering(saksbehandler: SaksbehandlerDto): AvventerTotrinnsvurdering =
            AvventerTotrinnsvurdering(saksbehandler = saksbehandler, tidspunkt = LocalDateTime.now())

        fun totrinnsvurderingRetur(
            notat: NotatDto,
            saksbehandler: SaksbehandlerDto,
            dialogRef: Long,
        ): TotrinnsvurderingRetur =
            TotrinnsvurderingRetur(notat = notat, saksbehandler = saksbehandler, tidspunkt = LocalDateTime.now(), dialogRef = dialogRef)

        fun totrinnsvurderingAutomatiskRetur(): TotrinnsvurderingAutomatiskRetur =
            TotrinnsvurderingAutomatiskRetur(tidspunkt = LocalDateTime.now())

        fun automatiskBehandlingStanset(): AutomatiskBehandlingStanset = AutomatiskBehandlingStanset(tidspunkt = LocalDateTime.now())

        fun vedtaksperiodeReberegnet(): VedtaksperiodeReberegnet = VedtaksperiodeReberegnet(tidspunkt = LocalDateTime.now())
    }
}

data class NotatDto(
    val oppgaveId: Long,
    val tekst: String,
)

data class LagtPåVent(
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
    override val dialogRef: Long,
    val årsaker: List<PåVentÅrsak>,
    val notattekst: String?,
    val frist: LocalDate,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null

    override fun toJson(): String =
        mapOf(
            "årsaker" to årsaker.map { it },
            "frist" to frist,
            "notattekst" to notattekst,
        ).let { objectMapper.writeValueAsString(it) }
}

private val objectMapper =
    jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())

data class FjernetFraPåVent(
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val dialogRef: Long? = null
}

data class TotrinnsvurderingFerdigbehandlet(
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val dialogRef: Long? = null
}

data class AvventerTotrinnsvurdering(
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val dialogRef: Long? = null
}

data class TotrinnsvurderingRetur(
    override val notat: NotatDto,
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
    override val dialogRef: Long,
) : HistorikkinnslagDto

data class TotrinnsvurderingAutomatiskRetur(
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val dialogRef: Long? = null
    override val saksbehandler: SaksbehandlerDto? = null
}

data class AutomatiskBehandlingStanset(
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val dialogRef: Long? = null
    override val saksbehandler: SaksbehandlerDto? = null
}

data class VedtaksperiodeReberegnet(
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val dialogRef: Long? = null
    override val saksbehandler: SaksbehandlerDto? = null
}
