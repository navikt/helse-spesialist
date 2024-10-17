package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalPeriodehistorikkDao(private val session: Session) : PeriodehistorikkRepository {
    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        throw UnsupportedOperationException()
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

    override fun lagre(
        historikkType: PeriodehistorikkType,
        saksbehandlerOid: UUID?,
        oppgaveId: Long,
        notatId: Int?,
        json: String,
    ) {
        @Language("PostgreSQL")
        val statement = """
                 SELECT utbetaling_id FROM oppgave WHERE id = :oppgaveId;
        """
        val utbetalingId =
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "oppgaveId" to oppgaveId,
                    ),
                ).map { it.uuid("utbetaling_id") }.asSingle,
            )
        utbetalingId?.let {
            lagre(historikkType, saksbehandlerOid, utbetalingId, notatId, json)
        } ?: throw IllegalStateException("Forventer å finne utbetaling for oppgave med id=$oppgaveId")
    }
}
