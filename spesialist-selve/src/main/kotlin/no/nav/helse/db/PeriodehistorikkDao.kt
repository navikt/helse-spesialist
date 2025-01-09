package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import java.util.UUID

interface PeriodehistorikkDao {
    fun lagreMedOppgaveId(
        historikkinnslag: Historikkinnslag,
        oppgaveId: Long,
    )

    fun lagre(
        historikkinnslag: Historikkinnslag,
        generasjonId: UUID,
    )
}
