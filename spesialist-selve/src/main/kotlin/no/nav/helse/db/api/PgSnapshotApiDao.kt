package no.nav.helse.db.api

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
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class PgSnapshotApiDao(private val dataSource: DataSource) : SnapshotApiDao {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun hentSnapshotMedMetadata(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                """
                SELECT * FROM person AS p
                INNER JOIN person_info as pi ON pi.id = p.info_ref
                INNER JOIN snapshot AS s ON s.person_ref = p.id
                WHERE p.fødselsnummer = :fnr;
                """.trimIndent()
            session.run(
                queryOf(
                    statement,
                    mapOf("fnr" to fødselsnummer),
                ).map { row ->
                    val personinfo =
                        Personinfo(
                            fornavn = row.string("fornavn"),
                            mellomnavn = row.stringOrNull("mellomnavn"),
                            etternavn = row.string("etternavn"),
                            fodselsdato = row.localDate("fodselsdato"),
                            kjonn = row.stringOrNull("kjonn")?.let(Kjonn::valueOf) ?: Kjonn.Ukjent,
                            adressebeskyttelse = row.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf),
                            unntattFraAutomatisering = null, // denne settes i query
                            reservasjon = null, // denne settes i query
                            fullmakt = null, // denne settes i query
                        )
                    val snapshot = objectMapper.readValue<GraphQLPerson>(row.string("data"))
                    personinfo to snapshot
                }.asSingle,
            )
        }

    override fun lagre(
        fødselsnummer: String,
        snapshot: GraphQLPerson,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { tx ->
            val personRef = tx.finnPersonRef(fødselsnummer)
            val versjon = snapshot.versjon
            if (versjon > tx.finnGlobalVersjon()) {
                tx.oppdaterGlobalVersjon(versjon)
            }
            val snapshotJson = objectMapper.writeValueAsString(snapshot)
            if (!snapshotJson.contains(fødselsnummer)) {
                sikkerLogg.warn("Henter snapshot for fnr $fødselsnummer, mottar snapshot: $snapshotJson")
            }
            tx.lagre(personRef, snapshotJson, versjon)
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
    val statement = "SELECT id FROM person WHERE fødselsnummer = ?"
    return requireNotNull(this.run(queryOf(statement, fødselsnummer).map { it.int("id") }.asSingle))
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
