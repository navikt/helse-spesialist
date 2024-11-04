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
    val saksbehandler: SaksbehandlerDto?
    val tidspunkt: LocalDateTime

    fun toJson(): String = "{}"

    companion object {
        fun lagtPåVentInnslag(
            notat: NotatDto?,
            saksbehandler: SaksbehandlerDto,
            årsaker: List<PåVentÅrsak>,
            frist: LocalDate,
        ): LagtPåVent =
            LagtPåVent(
                notat = notat,
                saksbehandler = saksbehandler,
                årsaker = årsaker,
                tidspunkt = LocalDateTime.now(),
                frist = frist,
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
        ): TotrinnsvurderingRetur = TotrinnsvurderingRetur(notat = notat, saksbehandler = saksbehandler, tidspunkt = LocalDateTime.now())

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
    override val notat: NotatDto?,
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
    val årsaker: List<PåVentÅrsak>,
    val frist: LocalDate,
) : HistorikkinnslagDto {
    override fun toJson(): String =
        mapOf(
            "årsaker" to årsaker.map { it },
            "frist" to frist,
            "notattekst" to notat?.tekst,
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
}

data class TotrinnsvurderingFerdigbehandlet(
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
}

data class AvventerTotrinnsvurdering(
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
}

data class TotrinnsvurderingRetur(
    override val notat: NotatDto,
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto

data class TotrinnsvurderingAutomatiskRetur(
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val saksbehandler: SaksbehandlerDto? = null
}

data class AutomatiskBehandlingStanset(
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val saksbehandler: SaksbehandlerDto? = null
}

data class VedtaksperiodeReberegnet(
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
    override val saksbehandler: SaksbehandlerDto? = null
}
