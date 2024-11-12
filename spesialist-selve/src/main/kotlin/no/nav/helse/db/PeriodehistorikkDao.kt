package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import java.util.UUID

interface PeriodehistorikkDao {
    fun lagreMedOppgaveId(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    )

    fun lagreMedGenerasjonId(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
    )

    fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    )
}
