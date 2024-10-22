package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.periodehistorikk.AvventerTotrinnsvurdering
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingFerdigbehandlet
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalPeriodehistorikkDao(
    private val session: Session,
) : HistorikkinnslagRepository {
    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ): Unit = throw UnsupportedOperationException()

    internal fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        generasjonId: UUID,
        notatId: Int?,
    ) {
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO periodehistorikk (type, saksbehandler_oid, generasjon_id, utbetaling_id, notat_id, json)
                VALUES (:type, :saksbehandler_oid, :generasjon_id, null, :notat_id, :json::json)
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "type" to historikkinnslag.type(),
                    "saksbehandler_oid" to historikkinnslag.saksbehandler?.oid,
                    "generasjon_id" to generasjonId,
                    "notat_id" to notatId,
                    "json" to historikkinnslag.toJson(),
                ),
            ).asUpdate,
        )
    }

    private fun HistorikkinnslagDto.type() =
        when (this) {
            is LagtPåVent -> "LEGG_PA_VENT"
            is FjernetFraPåVent -> "FJERN_FRA_PA_VENT" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingFerdigbehandlet -> "TOTRINNSVURDERING_ATTESTERT" // TODO: Mangler å migrere typen i databasen
            is AvventerTotrinnsvurdering -> "TOTRINNSVURDERING_TIL_GODKJENNING" // TODO: Mangler å migrere typen i databasen
        }

    override fun lagre(
        historikkType: PeriodehistorikkType,
        saksbehandlerOid: UUID?,
        utbetalingId: UUID,
        notatId: Int?,
        json: String,
    ) {
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO periodehistorikk (type, saksbehandler_oid, utbetaling_id, notat_id, json)
                VALUES (:type, :saksbehandler_oid, :utbetaling_id, :notat_id, :json::json)
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "type" to historikkType.name,
                    "saksbehandler_oid" to saksbehandlerOid,
                    "utbetaling_id" to utbetalingId,
                    "notat_id" to notatId,
                    "json" to json,
                ),
            ).asUpdate,
        )
    }

    override fun migrer(
        tidligereUtbetalingId: UUID,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement = """
                UPDATE periodehistorikk 
                SET utbetaling_id = :utbetalingId
                WHERE utbetaling_id = :tidligereUtbetalingId
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "utbetalingId" to utbetalingId,
                    "tidligereUtbetalingId" to tidligereUtbetalingId,
                ),
            ).asUpdate,
        )
    }
}
