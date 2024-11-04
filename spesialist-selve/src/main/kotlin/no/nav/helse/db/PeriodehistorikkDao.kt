package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import java.util.UUID

interface PeriodehistorikkDao {
    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
        dialogRef: Long?,
    )

    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
        dialogRef: Long?,
    )

    fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    )
}
