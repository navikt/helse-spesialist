package no.nav.helse.db

import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.Innslagstype
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import javax.sql.DataSource

interface PeriodehistorikkRepository {
    fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    )
}

class Periodehistorikk(
    dataSource: DataSource,
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

    private fun Innslagstype.tilPeriodehistorikkType() =
        when (this) {
            Innslagstype.LAGT_PA_VENT -> PeriodehistorikkType.LEGG_PA_VENT
            Innslagstype.FJERNET_FRA_PA_VENT -> PeriodehistorikkType.FJERN_FRA_PA_VENT
        }
}
