package no.nav.helse.spesialist.api.graphql.schema

data class Opptegnelse(
    val aktorId: String,
    val sekvensnummer: Int,
    val type: Opptegnelsetype,
    val payload: String,
)

enum class Opptegnelsetype {
    UTBETALING_ANNULLERING_FEILET,
    UTBETALING_ANNULLERING_OK,
    FERDIGBEHANDLET_GODKJENNINGSBEHOV,
    NY_SAKSBEHANDLEROPPGAVE,
    REVURDERING_AVVIST,
    REVURDERING_FERDIGBEHANDLET,
    PERSONDATA_OPPDATERT,
}
