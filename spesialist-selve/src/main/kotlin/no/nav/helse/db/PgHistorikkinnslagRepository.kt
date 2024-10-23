package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import java.util.UUID
import javax.sql.DataSource

class PgHistorikkinnslagRepository(
    private val dataSource: DataSource,
) : HistorikkinnslagRepository {
    private val oppgaveDao = OppgaveDao(dataSource)

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        val generasjonId = oppgaveDao.finnGenerasjonId(oppgaveId)
        lagre(historikkinnslag, generasjonId)
    }

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            TransactionalPeriodehistorikkDao(session).lagre(historikkinnslag, generasjonId)
        }
    }

    override fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalPeriodehistorikkDao(session).migrer(tidligereUtbetalingId, utbetalingId)
        }
    }

    override fun lagre(
        historikkType: PeriodehistorikkType,
        saksbehandlerOid: UUID?,
        utbetalingId: UUID,
        notatId: Int?,
        json: String,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalPeriodehistorikkDao(session).lagre(
                historikkType,
                saksbehandlerOid,
                utbetalingId,
                notatId,
                json,
            )
        }
    }
}
