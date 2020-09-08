package no.nav.helse.modell.feilhåndtering

data class FeilDto(
    val feilkode : String
) {
    val kildesystem : String =  "spesialist"
}
