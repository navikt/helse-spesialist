package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("Opptegnelse")
data class ApiOpptegnelse(
    val aktorId: String,
    val sekvensnummer: Int,
    val type: ApiOpptegnelsetype,
    val payload: String,
)

@GraphQLName("Opptegnelsetype")
enum class ApiOpptegnelsetype {
    UTBETALING_ANNULLERING_FEILET,
    UTBETALING_ANNULLERING_OK,
    FERDIGBEHANDLET_GODKJENNINGSBEHOV,
    NY_SAKSBEHANDLEROPPGAVE,
    REVURDERING_AVVIST,
    REVURDERING_FERDIGBEHANDLET,
    PERSONDATA_OPPDATERT,
    PERSON_KLAR_TIL_BEHANDLING,
}
