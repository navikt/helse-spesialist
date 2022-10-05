package no.nav.helse.spesialist.api.abonnement

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
    NY_SAKSBEHANDLEROPPGAVE,
    REVURDERING_AVVIST,
    REVURDERING_FERDIGBEHANDLET,
    PERSONDATA_OPPDATERT,
}
