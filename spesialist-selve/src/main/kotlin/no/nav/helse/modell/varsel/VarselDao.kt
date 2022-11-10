package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource): VarselRepository {

    override fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun nyttVarsel(vedtaksperiodeId: UUID, varselkode: String) {
        TODO("Not yet implemented")
    }

    override fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String) {
        TODO("Not yet implemented")
    }

    internal fun transaction(transactionBlock: (transactionalSession: TransactionalSession) -> Unit) {
        sessionOf(dataSource).use { session -> session.transaction(transactionBlock) }
    }

    internal fun alleVarslerForVedtaksperiode(vedtaksperiodeId: UUID): List<Pair<String, String>> {
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

    internal fun alleDefinisjoner(): List<Varseldefinisjon> {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM api_varseldefinisjon;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query
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
                }.asList
            )
        }
    }

    internal fun definisjonForId(id: UUID): Varseldefinisjon? {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM api_varseldefinisjon WHERE unik_id = ?;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query,
                    id
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

    internal fun lagreVarsel(
        id: UUID,
        kode: String,
        tidsstempel: LocalDateTime,
        vedtaksperiodeId: UUID,
        transactionalSession: TransactionalSession
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet) VALUES (?, ?, ?, ?);"

        transactionalSession.run(queryOf(query, id, kode, vedtaksperiodeId, tidsstempel).asUpdate)
    }

    internal fun lagreDefinisjon(
        id: UUID,
        kode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
        transactionalSession: TransactionalSession
    ) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING;"

        transactionalSession.run(queryOf(query, id, kode, tittel, forklaring, handling, avviklet, opprettet).asUpdate)
    }
}