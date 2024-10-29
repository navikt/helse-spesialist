package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStanset
import no.nav.helse.modell.periodehistorikk.AvventerTotrinnsvurdering
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingFerdigbehandlet
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingRetur
import no.nav.helse.modell.periodehistorikk.VedtaksperiodeReberegnet
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import java.util.UUID
import javax.sql.DataSource

class PgPeriodehistorikkDao(
    private val queryRunner: QueryRunner,
) : PeriodehistorikkDao, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        val generasjonId = PgOppgaveDao(queryRunner).finnGenerasjonId(oppgaveId)
        lagre(historikkinnslag, generasjonId)
    }

    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
    ) {
        when (historikkinnslag) {
            is FjernetFraPåVent -> lagre(historikkinnslag, generasjonId, null)
            is LagtPåVent -> {
                val notatId =
                    historikkinnslag.notat?.let { notat ->
                        PgNotatDao(queryRunner)
                            .lagreForOppgaveId(
                                oppgaveId = notat.oppgaveId,
                                tekst = notat.tekst,
                                saksbehandlerOid = historikkinnslag.saksbehandler.oid,
                                notatType = NotatType.PaaVent,
                            )?.toInt()
                    }
                lagre(historikkinnslag, generasjonId, notatId)
            }

            is TotrinnsvurderingFerdigbehandlet -> lagre(historikkinnslag, generasjonId, null)
            is AvventerTotrinnsvurdering -> lagre(historikkinnslag, generasjonId, null)
            is TotrinnsvurderingAutomatiskRetur -> lagre(historikkinnslag, generasjonId, null)
            is TotrinnsvurderingRetur -> {
                val notatId =
                    PgNotatDao(queryRunner)
                        .lagreForOppgaveId(
                            oppgaveId = historikkinnslag.notat.oppgaveId,
                            tekst = historikkinnslag.notat.tekst,
                            saksbehandlerOid = historikkinnslag.saksbehandler.oid,
                            notatType = NotatType.Retur,
                        )?.toInt()
                lagre(historikkinnslag, generasjonId, notatId)
            }

            is AutomatiskBehandlingStanset -> lagre(historikkinnslag, generasjonId, null)
            is VedtaksperiodeReberegnet -> lagre(historikkinnslag, generasjonId, null)
        }
    }

    internal fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
        notatId: Int?,
    ) {
        asSQL(
            """
                INSERT INTO periodehistorikk (type, saksbehandler_oid, generasjon_id, utbetaling_id, notat_id, json)
                VALUES (:type, :saksbehandler_oid, :generasjon_id, null, :notat_id, :json::json)
        """,
            "type" to historikkinnslag.type(),
            "saksbehandler_oid" to historikkinnslag.saksbehandler?.oid,
            "generasjon_id" to generasjonId,
            "notat_id" to notatId,
            "json" to historikkinnslag.toJson(),
        ).update()
    }

    private fun HistorikkinnslagDto.type() =
        when (this) {
            is LagtPåVent -> "LEGG_PA_VENT"
            is FjernetFraPåVent -> "FJERN_FRA_PA_VENT" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingFerdigbehandlet -> "TOTRINNSVURDERING_ATTESTERT" // TODO: Mangler å migrere typen i databasen
            is AvventerTotrinnsvurdering -> "TOTRINNSVURDERING_TIL_GODKJENNING" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingRetur -> "TOTRINNSVURDERING_RETUR" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingAutomatiskRetur -> "TOTRINNSVURDERING_RETUR" // TODO: Mangler å migrere typen i databasen
            is AutomatiskBehandlingStanset -> "STANS_AUTOMATISK_BEHANDLING" // TODO: Mangler å migrere typen i databasen
            is VedtaksperiodeReberegnet -> "VEDTAKSPERIODE_REBEREGNET" // TODO: Mangler å migrere typen i databasen
        }

    override fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    ) {
        asSQL(
            """
                UPDATE periodehistorikk 
                SET utbetaling_id = :utbetalingId
                WHERE utbetaling_id = :tidligereUtbetalingId
        """,
            "utbetalingId" to utbetalingId,
            "tidligereUtbetalingId" to tidligereUtbetalingId,
        ).update()
    }
}