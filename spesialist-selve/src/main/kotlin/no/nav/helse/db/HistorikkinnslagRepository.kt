package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import java.util.UUID

interface HistorikkinnslagRepository {
    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    )

    fun lagre(
        historikkType: PeriodehistorikkType,
        saksbehandlerOid: UUID? = null,
        utbetalingId: UUID,
        notatId: Int? = null,
        json: String = "{}",
    )

    fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    )
}
