package no.nav.helse.migrering.db

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class SpesialistDao(private val dataSource: DataSource) {

    internal fun forkast(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = "UPDATE vedtak SET forkastet = ?, forkastet_av_hendelse = ? WHERE vedtaksperiode_id = ? "
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, true, dummyForkastetAvHendelseId, vedtaksperiodeId).asUpdate)
        }
    }

    private companion object {
        private val dummyForkastetAvHendelseId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}