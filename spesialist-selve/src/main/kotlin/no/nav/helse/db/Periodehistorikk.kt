package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.Innslagstype
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatApiDao
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

    fun lagre(
        historikkType: PeriodehistorikkType,
        saksbehandlerOid: UUID? = null,
        oppgaveId: Long,
        notatId: Int? = null,
        json: String = "{}",
    )
}

class Periodehistorikk(
    private val dataSource: DataSource,
) : PeriodehistorikkRepository {
    private val notatDao: NotatApiDao = NotatApiDao(dataSource)

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        when (historikkinnslag) {
            is FjernetFraPåVent ->
                lagre(
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
                lagre(
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
        oppgaveId: Long,
        notatId: Int?,
        json: String,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalPeriodehistorikkDao(session).lagre(
                historikkType,
                saksbehandlerOid,
                oppgaveId,
                notatId,
                json,
            )
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

    private fun Innslagstype.tilPeriodehistorikkType() =
        when (this) {
            Innslagstype.LAGT_PA_VENT -> PeriodehistorikkType.LEGG_PA_VENT
            Innslagstype.FJERNET_FRA_PA_VENT -> PeriodehistorikkType.FJERN_FRA_PA_VENT
        }
}
