package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.SnapshotRepository
import no.nav.helse.objectMapper
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class SnapshotDao(private val dataSource: DataSource) : SnapshotRepository {
    override fun lagre(
        fødselsnummer: String,
        snapshot: GraphQLPerson,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { tx ->
                val personRef = tx.finnPersonRef(fødselsnummer)
                val versjon = snapshot.versjon
                if (versjon > tx.finnGlobalVersjon()) {
                    tx.oppdaterGlobalVersjon(versjon)
                }
                tx.lagre(personRef, objectMapper.writeValueAsString(snapshot), versjon)
            }
        }
    }

    private fun TransactionalSession.lagre(
        personRef: Int,
        snapshot: String,
        versjon: Int,
    ): Int {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO snapshot(data, versjon, person_ref)
                VALUES(CAST(:snapshot as json), :versjon, :person_ref)
            ON CONFLICT(person_ref) DO UPDATE
                SET data = CAST(:snapshot as json), versjon = :versjon;
        """
        return requireNotNull(
            run(
                queryOf(
                    statement,
                    mapOf(
                        "snapshot" to snapshot,
                        "versjon" to versjon,
                        "person_ref" to personRef,
                    ),
                ).asUpdateAndReturnGeneratedKey,
            ),
        ).toInt()
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
