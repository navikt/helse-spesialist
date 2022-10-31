package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource) {

    internal fun alleVarslerForVedtaksperiode(vedtaksperiodeId: UUID): List<Pair<String, String>> {
        @Language("PostgreSQL")
        val alleVarslerForVedtaksperiode = """
            select unik_id, kode from selve_varsel where vedtaksperiode_id = ?
            """

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    alleVarslerForVedtaksperiode,
                    vedtaksperiodeId
                ).map { varsel -> Pair(varsel.string("unik_id"), varsel.string("kode")) }.asList
            )
        }
    }

    internal fun transaction(transactionBlock: (transactionalSession: TransactionalSession) -> Unit) {
        sessionOf(dataSource).use { session -> session.transaction(transactionBlock) }
    }

    internal fun lagre(
        id: UUID,
        kode: String,
        tidsstempel: LocalDateTime,
        vedtaksperiodeId: UUID,
        transactionalSession: TransactionalSession
    ) {
        @Language("PostgreSQL")
        val query = "insert into selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet) values (?, ?, ?, ?);"

        transactionalSession.run(queryOf(query, id, kode, vedtaksperiodeId, tidsstempel).asUpdate)
    }
}