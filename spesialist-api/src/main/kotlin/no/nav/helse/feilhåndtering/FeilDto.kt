package no.nav.helse.feilhåndtering

data class FeilDto(
    val feilkode: String,
    val kontekst: Map<String, Any>
) {
    val kildesystem: String = "spesialist"
}
