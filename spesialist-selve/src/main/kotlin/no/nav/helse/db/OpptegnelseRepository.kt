package no.nav.helse.db

import java.util.UUID

interface OpptegnelseRepository {
    fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: String,
        type: OpptegnelseType,
    )

    fun finnOpptegnelser(saksbehandlerIdent: UUID): List<Opptegnelse>

    data class Opptegnelse(
        val aktorId: String,
        val sekvensnummer: Int,
        val type: OpptegnelseType,
        val payload: String,
    )

    enum class OpptegnelseType {
        UTBETALING_ANNULLERING_FEILET,
        UTBETALING_ANNULLERING_OK,
        FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        NY_SAKSBEHANDLEROPPGAVE,
        REVURDERING_AVVIST,
        REVURDERING_FERDIGBEHANDLET,
        PERSONDATA_OPPDATERT,
        PERSON_KLAR_TIL_BEHANDLING,
    }
}
