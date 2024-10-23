package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import java.util.UUID

interface HistorikkinnslagRepository {
    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    )

    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
    )

    fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    )
}
