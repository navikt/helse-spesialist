package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.varsel.Varsel
import org.intellij.lang.annotations.Language

class GenerasjonDao(private val dataSource: DataSource) {

    internal fun finnSisteFor(vedtaksperiodeId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query =
            "SELECT id, unik_id, vedtaksperiode_id, låst FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id DESC;"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map(::toGenerasjon).asSingle)
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

    internal fun utbetalingFor(vedtaksperiodeId: UUID, utbetalingId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET utbetaling_id = ? 
            WHERE vedtaksperiode_id = ? AND låst = false
            RETURNING unik_id, vedtaksperiode_id, låst;
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, utbetalingId, vedtaksperiodeId).map {
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
            RETURNING id, unik_id, vedtaksperiode_id, låst
            """

        return requireNotNull(sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, hendelseId).map(::toGenerasjon).asSingle)
        }) { "Kunne ikke opprette ny vedtaksperiode generasjon" }
    }

    private fun toGenerasjon(row: Row): Generasjon {
        return Generasjon(
            row.uuid("unik_id"),
            row.uuid("vedtaksperiode_id"),
            row.boolean("låst"),
            varslerFor(row.long("id")).toSet()
        )
    }

    private fun varslerFor(generasjonRef: Long): List<Varsel> {
        @Language("PostgreSQL")
        val query = "SELECT unik_id, vedtaksperiode_id, kode, opprettet, status FROM selve_varsel WHERE generasjon_ref = ?"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonRef).map {
                Varsel(
                    it.uuid("unik_id"),
                    it.string("kode"),
                    it.localDateTime("opprettet"),
                    it.uuid("vedtaksperiode_id"),
                    enumValueOf(it.string("status"))
                )
            }.asList)
        }
    }
}