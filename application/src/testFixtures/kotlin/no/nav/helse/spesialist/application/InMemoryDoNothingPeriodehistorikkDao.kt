package no.nav.helse.spesialist.application

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import java.util.UUID

class InMemoryDoNothingPeriodehistorikkDao : PeriodehistorikkDao {
    override fun lagreMedOppgaveId(
        historikkinnslag: Historikkinnslag,
        oppgaveId: Long,
    ) {
    }

    override fun lagre(
        historikkinnslag: Historikkinnslag,
        behandlingId: UUID,
    ) {
    }
}
