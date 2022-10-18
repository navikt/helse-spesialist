package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class GenerasjonDao(private val dataSource: DataSource) {
    internal fun finnUlåstEllerOpprett(vedtaksperiodeId: UUID): Long {
        return sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction {
                it.finnUlåst(vedtaksperiodeId) ?: it.lagre(vedtaksperiodeId)
            }
        }
    }

    internal fun lås(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query =
            "UPDATE selve_vedtaksperiode_generasjon SET låst = true, låst_tidspunkt = now() WHERE vedtaksperiode_id = ? AND låst = false;"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).asUpdate)
        }
    }

    private fun TransactionalSession.finnUlåst(vedtaksperiodeId: UUID): Long? {
        @Language("PostgreSQL")
        val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = false;"
        return run(queryOf(query, vedtaksperiodeId).map { it.long("id") }.asSingle)
    }

    private fun TransactionalSession.lagre(vedtaksperiodeId: UUID): Long {
        @Language("PostgreSQL")
        val query = "INSERT INTO selve_vedtaksperiode_generasjon (vedtaksperiode_id) values (?);"
        return requireNotNull(
            run(
                queryOf(
                    query,
                    vedtaksperiodeId
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }
}