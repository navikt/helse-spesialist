package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.periodehistorikk.AvventerTotrinnsvurdering
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingFerdigbehandlet
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import java.util.UUID
import javax.sql.DataSource

class PgHistorikkinnslagRepository(
    private val dataSource: DataSource,
) : HistorikkinnslagRepository {
    private val notatDao: NotatApiDao = NotatApiDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        val generasjonId = oppgaveDao.finnGenerasjonId(oppgaveId)
        when (historikkinnslag) {
            is FjernetFraPåVent -> lagre(historikkinnslag, generasjonId, null)
            is LagtPåVent -> {
                val notatId =
                    historikkinnslag.notat?.let { notat ->
                        notatDao
                            .opprettNotatForOppgaveId(
                                oppgaveId = notat.oppgaveId,
                                tekst = notat.tekst,
                                saksbehandlerOid = historikkinnslag.saksbehandler.oid,
                                type = NotatType.PaaVent,
                            )?.toInt()
                    }
                lagre(historikkinnslag, generasjonId, notatId)
            }
            is TotrinnsvurderingFerdigbehandlet -> lagre(historikkinnslag, generasjonId, null)
            is AvventerTotrinnsvurdering -> lagre(historikkinnslag, generasjonId, null)
        }
    }

    private fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
        notatId: Int?,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalPeriodehistorikkDao(session).lagre(historikkinnslag, generasjonId, notatId)
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
