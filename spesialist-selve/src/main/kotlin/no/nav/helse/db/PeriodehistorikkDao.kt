package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import java.util.UUID

interface PeriodehistorikkDao {
    fun lagreMedOppgaveId(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    )

    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
    )
}
