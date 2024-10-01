package no.nav.helse.db

import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType

interface OpptegnelseRepository {
    fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: OpptegnelsePayload,
        type: OpptegnelseType,
    )
}
