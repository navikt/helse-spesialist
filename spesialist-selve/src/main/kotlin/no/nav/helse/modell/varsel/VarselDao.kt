package no.nav.helse.modell.varsel

import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource) {

    internal fun lagre(id: UUID, kode: String, tittel: String, vedtaksperiodeId: UUID, utbetalingsId: UUID?){
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run {
                    val kodeId = oppdaterVarselKoder(kode)
                }
            }
        }
    }

    private fun TransactionalSession.oppdaterVarselKoder(kode: String): Long? {
        @Language("PostgreSQL")
        val varselKodeOppdatering = """
            INSERT INTO selve_varsel_kode(kode) VALUES(?)
            ON CONFLICT DO NOTHING
            RETURNING id
            """
        return run(
            queryOf(
                varselKodeOppdatering,
                kode
            ).asUpdateAndReturnGeneratedKey
        )
    }
}