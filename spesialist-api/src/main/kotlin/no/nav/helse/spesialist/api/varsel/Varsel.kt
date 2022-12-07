package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO.VarselvurderingDTO

data class Varsel(
    private val id: UUID,
    private val tittel: String,
    private val forklaring: String?,
    private val handling: String?,
    private val vurdering: Varselvurdering?,
) {
    internal companion object {
        internal fun List<Varsel>.toDto(): List<VarselDTO> {
            return map { it.toDto() }
        }
    }

    internal fun toDto() = VarselDTO(id.toString(), tittel, forklaring, handling, vurdering?.toDto())

    data class Varselvurdering(
        private val ident: String,
        private val tidsstempel: LocalDateTime,
        private val status: Varselstatus,
    ) {
        internal fun toDto() = VarselvurderingDTO(ident, tidsstempel.toString(), status)
    }

    enum class Varselstatus {
        VURDERT,
        GODKJENT,
        AVVIST,
    }
}