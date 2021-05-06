package no.nav.helse.modell.feilhåndtering

data class FeilDto(
    val feilkode: String,
    val kontekst: Map<String, Any>
) {
    val kildesystem: String = "spesialist"
}
