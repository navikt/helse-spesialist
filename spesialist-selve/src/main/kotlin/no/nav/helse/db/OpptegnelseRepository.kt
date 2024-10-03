package no.nav.helse.db

import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import java.util.UUID

interface OpptegnelseRepository {
    fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: OpptegnelsePayload,
        type: OpptegnelseType,
    )

    fun finnOpptegnelser(saksbehandlerIdent: UUID): List<Opptegnelse>
}
