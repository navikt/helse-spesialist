package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.LateIdAggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject

data class Sekvensnummer(
    val value: Int,
) : ValueObject

class Opptegnelse private constructor(
    id: Sekvensnummer?,
    val identitetsnummer: Identitetsnummer,
    val type: Type,
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
        ) = Opptegnelse(id = null, identitetsnummer = identitetsnummer, type = type)

        fun fraLagring(
            id: Sekvensnummer,
            identitetsnummer: Identitetsnummer,
            type: Type,
        ) = Opptegnelse(id = id, identitetsnummer = identitetsnummer, type = type)
    }
}
