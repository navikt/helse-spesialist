package no.nav.helse.abonnement

data class OpptegnelseDto(
    val aktørId: Long,
    val sekvensnummer: Int,
    val type: OpptegnelseType,
    val payload: String
)

enum class OpptegnelseType {
    UTBETALING_ANNULLERING_FEILET,
    UTBETALING_ANNULLERING_OK,
    FERDIGBEHANDLET_GODKJENNIGSBEHOV,
    NY_SAKSBEHANDLEROPPGAVE
}
