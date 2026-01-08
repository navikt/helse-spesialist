package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.LateIdAggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject

@JvmInline
value class Sekvensnummer(
    val value: Int,
) : ValueObject

class Opptegnelse private constructor(
    id: Sekvensnummer?,
    val identitetsnummer: Identitetsnummer,
    val type: Type,
    val payload: String,
) : LateIdAggregateRoot<Sekvensnummer>(id) {
    enum class Type {
        UTBETALING_ANNULLERING_FEILET,
        UTBETALING_ANNULLERING_OK,
        FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        NY_SAKSBEHANDLEROPPGAVE,
        REVURDERING_AVVIST,
        REVURDERING_FERDIGBEHANDLET,
        PERSONDATA_OPPDATERT,
        PERSON_KLAR_TIL_BEHANDLING,
    }

    companion object {
        fun ny(
            identitetsnummer: Identitetsnummer,
            type: Type,
            payload: String,
        ) = Opptegnelse(id = null, identitetsnummer = identitetsnummer, type = type, payload = payload)

        fun fraLagring(
            id: Sekvensnummer,
            identitetsnummer: Identitetsnummer,
            type: Type,
            payload: String,
        ) = Opptegnelse(id = id, identitetsnummer = identitetsnummer, type = type, payload = payload)
    }
}
