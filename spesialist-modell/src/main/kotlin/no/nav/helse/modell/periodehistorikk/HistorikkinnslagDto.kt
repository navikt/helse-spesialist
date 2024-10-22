package no.nav.helse.modell.periodehistorikk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface HistorikkinnslagDto {
    val type: Innslagstype
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
                type = Innslagstype.LAGT_PA_VENT,
                notat = notat,
                saksbehandler = saksbehandler,
                årsak = årsaker,
                tidspunkt = LocalDateTime.now(),
                frist = frist,
            )

        fun fjernetFraPåVentInnslag(saksbehandler: SaksbehandlerDto): FjernetFraPåVent =
            FjernetFraPåVent(
                type = Innslagstype.FJERNET_FRA_PA_VENT,
                saksbehandler = saksbehandler,
                tidspunkt = LocalDateTime.now(),
            )
    }
}

data class NotatDto(
    val oppgaveId: Long,
    val tekst: String,
)

data class LagtPåVent(
    override val type: Innslagstype,
    override val notat: NotatDto?,
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
    val årsak: List<PåVentÅrsak>,
    val frist: LocalDate,
) : HistorikkinnslagDto {
    override fun toJson(): String =
        mapOf(
            "årsaker" to årsak.map { it },
            "frist" to frist,
        ).let { objectMapper.writeValueAsString(it) }
}

private val objectMapper =
    jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())

data class FjernetFraPåVent(
    override val type: Innslagstype,
    override val saksbehandler: SaksbehandlerDto,
    override val tidspunkt: LocalDateTime,
) : HistorikkinnslagDto {
    override val notat: NotatDto? = null
}

enum class Innslagstype {
    LAGT_PA_VENT,
    FJERNET_FRA_PA_VENT,
}
