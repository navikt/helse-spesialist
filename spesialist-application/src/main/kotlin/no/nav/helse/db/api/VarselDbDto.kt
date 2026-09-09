package no.nav.helse.db.api

import java.time.LocalDateTime
import java.util.UUID

data class VarselDbDto(
    val varselId: UUID,
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val kode: String,
    var status: Varselstatus,
    val varseldefinisjon: VarseldefinisjonDbDto?,
    val varselvurdering: VarselvurderingDbDto?,
) {
    enum class Varselstatus {
        INAKTIV,
        AKTIV,
        AVVIKLET,

        // Varsler er 'VURDERT' når saksbehandler har trykket på avkrysningsboksen i Speil
        VURDERT,

        // Varsler er 'GODKJENT' når behandlingen de tilhører er godkjent/ferdigbehandlet
        GODKJENT,
        AVVIST,
    }

    data class VarselvurderingDbDto(
        val ident: String,
        val tidsstempel: LocalDateTime,
    )

    data class VarseldefinisjonDbDto(
        val definisjonId: UUID,
        val tittel: String,
        val forklaring: String?,
        val handling: String?,
    )
}
