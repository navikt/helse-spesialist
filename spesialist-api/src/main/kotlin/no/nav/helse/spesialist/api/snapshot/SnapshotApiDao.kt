package no.nav.helse.spesialist.api.snapshot

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.graphql.schema.Adressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class SnapshotApiDao(private val dataSource: DataSource) {
    fun hentSnapshotMedMetadata(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                """
                SELECT * FROM person AS p
                INNER JOIN person_info as pi ON pi.id = p.info_ref
                INNER JOIN snapshot AS s ON s.person_ref = p.id
                WHERE p.fodselsnummer = :fnr;
                """.trimIndent()
            session.run(
                queryOf(
                    statement,
                    mapOf("fnr" to fødselsnummer.toLong()),
                ).map { row ->
                    val personinfo =
                        Personinfo(
                            fornavn = row.string("fornavn"),
                            mellomnavn = row.stringOrNull("mellomnavn"),
                            etternavn = row.string("etternavn"),
                            fodselsdato = row.localDateOrNull("fodselsdato"),
                            kjonn = row.stringOrNull("kjonn")?.let(Kjonn::valueOf) ?: Kjonn.Ukjent,
                            adressebeskyttelse = row.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf),
                            reservasjon = null,
                        )
                    val snapshot = objectMapper.readValue<GraphQLPerson>(row.string("data"))
                    personinfo to snapshot
                }.asSingle,
            )
        }

    fun lagre(
        fødselsnummer: String,
        snapshot: GraphQLPerson,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { tx ->
            val personRef = tx.finnPersonRef(fødselsnummer)
            val versjon = snapshot.versjon
            if (versjon > tx.finnGlobalVersjon()) {
                tx.oppdaterGlobalVersjon(versjon)
            }
            tx.lagre(personRef, objectMapper.writeValueAsString(snapshot), versjon)
        }
    }

    fun utdatert(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val versjonForSnapshot = tx.finnSnapshotVersjon(fødselsnummer)
                val sisteGjeldendeVersjon = tx.finnGlobalVersjon()

                versjonForSnapshot?.let { it < sisteGjeldendeVersjon } ?: true
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

private fun TransactionalSession.finnSnapshotVersjon(fødselsnummer: String): Int? {
    @Language("PostgreSQL")
    val statement = """
            SELECT versjon FROM snapshot s
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
