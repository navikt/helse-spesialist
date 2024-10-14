package no.nav.helse.db

import no.nav.helse.spesialist.api.graphql.schema.NotatType
import java.util.UUID

interface NotatRepository {
    fun lagreForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandler_oid: UUID,
        notatType: NotatType = NotatType.Generelt,
    ): Long?
}
