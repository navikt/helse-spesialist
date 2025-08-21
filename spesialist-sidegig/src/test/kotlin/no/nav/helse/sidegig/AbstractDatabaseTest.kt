package no.nav.helse.sidegig

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import org.intellij.lang.annotations.Language

internal abstract class AbstractDatabaseTest {
    protected val dataSource = DBTestFixture.module.dataSource
    protected val behandlingDao = PgBehandlingDao(dataSource)

    fun insert(
        @Language("PostgreSQL") query: String,
        paramMap: Map<String, Any?> = emptyMap(),
    ) =
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, paramMap).asUpdate)
        }

    fun insertAndReturnGeneratedKey(
        @Language("PostgreSQL") query: String,
        paramMap: Map<String, Any?> = emptyMap(),
    ): Long? =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(queryOf(query, paramMap).asUpdateAndReturnGeneratedKey)
        }
}

