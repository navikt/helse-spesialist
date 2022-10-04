package no.nav.helse.modell.varsel

import java.util.UUID
import javax.sql.DataSource
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

    internal fun lagre(id: UUID, kode: String, vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val nyttVarsel = """
            insert into selve_varsel (unik_id, kode, vedtaksperiode_id)
            values (?,?,?);
            """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    nyttVarsel,
                    id,
                    kode,
                    vedtaksperiodeId
                ).asUpdate
            )
        }
    }
}