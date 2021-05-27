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
            tx.lagre(personRef, snapshot, versjon)
        }
    }

    fun utdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        session.transaction { tx ->
            val versjonForSnapshot = tx.finnSnapshotVersjon(fødselsnummer)
            val sisteGjeldendeVersjon = tx.finnGlobalVersjon()

            versjonForSnapshot?.let { it < sisteGjeldendeVersjon } ?: true
        }
    }

    private fun TransactionalSession.lagre(personRef: Int, snapshot: String, versjon: Int): Int {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO speil_snapshot(person_ref, data, versjon)
                VALUES(:person_ref, CAST(:snapshot as json), :versjon)
            ON CONFLICT(person_ref) DO UPDATE
                SET data = CAST(:snapshot as json), sist_endret = now(), versjon = :versjon;
        """
        return requireNotNull(
            run(
                queryOf(
                    statement,
                    mapOf(
                        "person_ref" to personRef,
                        "snapshot" to snapshot,
                        "versjon" to versjon
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        ).toInt()
    }

    private fun TransactionalSession.finnSnapshotVersjon(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val statement = """
            SELECT versjon FROM speil_snapshot s
                INNER JOIN person p on p.id = s.person_ref
            WHERE p.fodselsnummer = ?
        """
        return run(queryOf(statement, fødselsnummer.toLong()).map { it.int("versjon") }.asSingle)
    }

    private fun TransactionalSession.finnPersonRef(fødselsnummer: String): Int {
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
