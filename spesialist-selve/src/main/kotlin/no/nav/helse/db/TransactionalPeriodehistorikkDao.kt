package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.naming.OperationNotSupportedException

class TransactionalPeriodehistorikkDao(private val session: Session) : PeriodehistorikkRepository {
    override fun lagre(
        historikkinnslag: HistorikkinnslagDto,
        oppgaveId: Long,
    ) {
        throw OperationNotSupportedException()
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
}
