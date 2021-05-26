package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class SnapshotDao(private val dataSource: DataSource) {

    fun lagre(fødselsnummer: String, snapshot: String) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { tx ->
            val personRef = tx.finnPersonRef(fødselsnummer)

            @Language("PostgreSQL")
            val statement = """
            INSERT INTO speil_snapshot(person_ref, data)
                VALUES(:person_ref, CAST(:snapshot as json))
            ON CONFLICT(person_ref) DO UPDATE
                SET data = CAST(:snapshot as json), sist_endret = now();
        """
            requireNotNull(
                tx.run(
                    queryOf(
                        statement,
                        mapOf(
                            "person_ref" to personRef,
                            "snapshot" to snapshot
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }.toInt()
    }

    private fun TransactionalSession.finnPersonRef(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM person WHERE fodselsnummer = ?"
        return this.run(queryOf(statement, fødselsnummer.toLong()).map { it.int("id") }.asSingle)
    }
}
