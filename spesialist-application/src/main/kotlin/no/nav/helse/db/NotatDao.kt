package no.nav.helse.db

import no.nav.helse.spesialist.domain.NotatType
import java.util.UUID

interface NotatDao {
    fun lagreForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandlerOid: UUID,
        notatType: NotatType = NotatType.Generelt,
        dialogRef: Long,
    ): Long?
}
