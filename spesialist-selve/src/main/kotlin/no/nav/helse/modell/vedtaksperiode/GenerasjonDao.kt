package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class GenerasjonDao(private val dataSource: DataSource) {

    internal fun generasjon(vedtaksperiodeId: UUID): Long? {
        @Language("PostgreSQL")
        val query =
            "SELECT id FROM selve_vedtaksperiode_generasjon where vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1;"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.long("id") }.asSingle)
        }
    }

    internal fun låsFor(vedtaksperiodeId: UUID, hendelseId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET låst = true, låst_tidspunkt = now(), låst_av_hendelse = ? 
            WHERE vedtaksperiode_id = ? AND låst = false
            RETURNING unik_id, vedtaksperiode_id, låst;
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, hendelseId, vedtaksperiodeId).map {
                Generasjon(
                    it.uuid("unik_id"),
                    it.uuid("vedtaksperiode_id"),
                    it.boolean("låst"),
                )
            }.asSingle)
        }
    }

    internal fun finnSisteFor(vedtaksperiodeId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id, vedtaksperiode_id, låst FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id DESC;"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map {
                Generasjon(
                    it.uuid("unik_id"),
                    it.uuid("vedtaksperiode_id"),
                    it.boolean("låst"),
                )
            }.asSingle)
        }
    }

    internal fun opprettFor(vedtaksperiodeId: UUID, hendelseId: UUID): Generasjon {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon (vedtaksperiode_id, opprettet_av_hendelse) 
            VALUES (?, ?)
            RETURNING unik_id, vedtaksperiode_id, låst;
            """

        return requireNotNull(sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, hendelseId).map {
                Generasjon(
                    it.uuid("unik_id"),
                    it.uuid("vedtaksperiode_id"),
                    it.boolean("låst"),
                )
            }.asSingle)
        }) { "Kunne ikke opprette ny vedtaksperiode generasjon" }
    }
}