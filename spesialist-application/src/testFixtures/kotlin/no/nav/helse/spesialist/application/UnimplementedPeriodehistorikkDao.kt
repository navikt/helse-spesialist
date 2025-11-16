package no.nav.helse.spesialist.application

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import java.util.UUID

class UnimplementedPeriodehistorikkDao : PeriodehistorikkDao {
    override fun lagreMedOppgaveId(
        historikkinnslag: Historikkinnslag,
        oppgaveId: Long
    ) {
        TODO("Not yet implemented")
    }

    override fun lagre(
        historikkinnslag: Historikkinnslag,
        generasjonId: UUID
    ) {
        TODO("Not yet implemented")
    }
}
