package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class SnapshotDao(private val dataSource: DataSource) {

    fun lagre(fødselsnummer: String, snapshot: String) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { tx ->
            val personRef = tx.finnPersonRef(fødselsnummer)
            val versjon = objectMapper.readTree(snapshot)["versjon"].asInt()
            if (versjon > tx.finnGlobalVersjon())
                tx.oppdaterGlobalVersjon(versjon)

            @Language("PostgreSQL")
            val statement = """
            INSERT INTO speil_snapshot(person_ref, data, versjon)
                VALUES(:person_ref, CAST(:snapshot as json), :versjon)
            ON CONFLICT(person_ref) DO UPDATE
                SET data = CAST(:snapshot as json), sist_endret = now(), versjon = :versjon;
        """
            requireNotNull(
                tx.run(
                    queryOf(
                        statement,
                        mapOf(
                            "person_ref" to personRef,
                            "snapshot" to snapshot,
                            "versjon" to versjon
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
            )
        }.toInt()
    }

    private fun TransactionalSession.finnPersonRef(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM person WHERE fodselsnummer = ?"
        return requireNotNull(this.run(queryOf(statement, fødselsnummer.toLong()).map { it.int("id") }.asSingle))
    }

    private fun TransactionalSession.finnGlobalVersjon(): Int {
        @Language("PostgreSQL")
        val statement = "SELECT versjon FROM global_snapshot_versjon"
        return requireNotNull(this.run(queryOf(statement).map { it.int("versjon") }.asSingle))
    }

    private fun TransactionalSession.oppdaterGlobalVersjon(versjon: Int) {
        @Language("PostgreSQL")
        val statement = "UPDATE global_snapshot_versjon SET versjon = ?, sist_endret = now() WHERE id = 1"
        this.run(queryOf(statement, versjon).asExecute)
    }
}
