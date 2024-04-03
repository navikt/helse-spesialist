package no.nav.helse.spesialist.api.feilhåndtering

data class FeilDto(
    val feilkode: String,
    val kontekst: Map<String, Any>,
    val kildesystem: String = "spesialist",
)
