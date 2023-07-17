package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID

class VarselDto(
    val id: UUID,
    val varselkode: String,
    val opprettet: LocalDateTime,
    val vedtaksperiodeId: UUID,
    val status: VarselStatusDto
) {
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is VarselDto
                && id == other.id
                && varselkode == other.varselkode
                && opprettet.withNano(0) == other.opprettet.withNano(0)
                && vedtaksperiodeId == other.vedtaksperiodeId
                && status == other.status
            )

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}

enum class VarselStatusDto {
    AKTIV,
    INAKTIV,
    GODKJENT,
    VURDERT,
    AVVIST,
}