package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource) {

    internal fun alleVarslerFor(vedtaksperiodeId: UUID): List<Pair<String, String>> {
        @Language("PostgreSQL")
        val query = "SELECT unik_id,kode FROM selve_varsel WHERE vedtaksperiode_id = ?;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query,
                    vedtaksperiodeId
                ).map { varsel -> Pair(varsel.string("unik_id"), varsel.string("kode")) }.asList
            )
        }
    }

    internal fun definisjonFor(definisjonId: UUID): Varseldefinisjon? {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM api_varseldefinisjon WHERE unik_id = ?;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query,
                    definisjonId
                ).map {
                    Varseldefinisjon(
                        id = it.uuid("unik_id"),
                        kode = it.string("kode"),
                        tittel = it.string("tittel"),
                        forklaring = it.string("forklaring"),
                        handling = it.string("handling"),
                        avviklet = it.boolean("avviklet"),
                        opprettet = it.localDateTime("opprettet")
                    )
                }.asSingle
            )
        }
    }

    internal fun lagreVarsel(id: UUID, kode: String, tidsstempel: LocalDateTime, vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = "INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet) VALUES (?, ?, ?, ?);"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, id, kode, vedtaksperiodeId, tidsstempel).asUpdate)
        }
    }

    internal fun lagreDefinisjon(id: UUID, kode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING;"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, id, kode, tittel, forklaring, handling, avviklet, opprettet).asUpdate)
        }
    }
}