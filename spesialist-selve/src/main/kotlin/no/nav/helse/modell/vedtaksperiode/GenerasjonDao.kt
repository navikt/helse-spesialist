package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class GenerasjonDao(private val dataSource: DataSource) {

    internal fun generasjon(vedtaksperiodeId: UUID): Long? {
        @Language("PostgreSQL")
        val query = "SELECT id FROM selve_vedtaksperiode_generasjon where vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1;"

        return sessionOf(dataSource).use {session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.long("id") }.asSingle)
        }
    }

    internal fun prøvOpprett(vedtaksperiodeId: UUID, vedtaksperiodeEndretHendelseId: UUID) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction {
                if (it.åpenGenerasjon(vedtaksperiodeId) != null) return@transaction null
                it.opprettNyGenerasjon(vedtaksperiodeId, vedtaksperiodeEndretHendelseId)
            }
        }

    internal fun låsGenerasjon(vedtaksperiodeId: UUID, vedtakFattetId: UUID): Long? {
        @Language("PostgreSQL")
        val query =
            "UPDATE selve_vedtaksperiode_generasjon SET låst = true, låst_tidspunkt = now(), låst_av_hendelse = ? WHERE vedtaksperiode_id = ? AND låst = false;"

        return sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(queryOf(query, vedtakFattetId, vedtaksperiodeId).asUpdateAndReturnGeneratedKey)
        }
    }

    private fun TransactionalSession.åpenGenerasjon(vedtaksperiodeId: UUID): Long? {
        @Language("PostgreSQL")
        val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = false;"
        return run(queryOf(query, vedtaksperiodeId).map { it.long("id") }.asSingle)
    }

    private fun TransactionalSession.opprettNyGenerasjon(
        vedtaksperiodeId: UUID,
        vedtaksperiodeEndretHendelseId: UUID
    ): Long {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO selve_vedtaksperiode_generasjon (vedtaksperiode_id, opprettet_av_hendelse) values (?, ?);"

        return requireNotNull(
            run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    vedtaksperiodeEndretHendelseId
                ).asUpdateAndReturnGeneratedKey
            )
        ) { "Kunne ikke opprette ny vedtaksperiode generasjon" }
    }
}