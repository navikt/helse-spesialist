package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.Innslagstype
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import java.util.UUID
import javax.sql.DataSource

interface PeriodehistorikkRepository {
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
}

class Periodehistorikk(
    private val dataSource: DataSource,
) : PeriodehistorikkRepository {
    private val periodehistorikkDao: PeriodehistorikkApiDao = PeriodehistorikkApiDao(dataSource)
    private val notatDao: NotatApiDao = NotatApiDao(dataSource)

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        when (historikkinnslag) {
            is FjernetFraPåVent ->
                periodehistorikkDao.lagre(
                    historikkType = historikkinnslag.type.tilPeriodehistorikkType(),
                    saksbehandlerOid = historikkinnslag.saksbehandler.oid,
                    oppgaveId = oppgaveId,
                    notatId = null,
                )

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
                periodehistorikkDao.lagre(
                    historikkType = historikkinnslag.type.tilPeriodehistorikkType(),
                    saksbehandlerOid = historikkinnslag.saksbehandler.oid,
                    oppgaveId = oppgaveId,
                    notatId = notatId,
                    json = historikkinnslag.toJson(),
                )
            }
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
            session.transaction { transaction ->
                TransactionalPeriodehistorikkDao(transaction).lagre(
                    historikkType,
                    saksbehandlerOid,
                    utbetalingId,
                    notatId,
                    json,
                )
            }
        }
    }

    private fun Innslagstype.tilPeriodehistorikkType() =
        when (this) {
            Innslagstype.LAGT_PA_VENT -> PeriodehistorikkType.LEGG_PA_VENT
            Innslagstype.FJERNET_FRA_PA_VENT -> PeriodehistorikkType.FJERN_FRA_PA_VENT
        }
}
