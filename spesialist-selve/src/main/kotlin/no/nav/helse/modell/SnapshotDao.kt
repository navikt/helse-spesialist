package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class SnapshotDao(private val dataSource: DataSource) {
    fun lagre(fødselsnummer: String, snapshot: GraphQLPerson) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { tx ->
                val personRef = tx.finnPersonRef(fødselsnummer)
                val versjon = snapshot.versjon
                if (versjon > tx.finnGlobalVersjon())
                    tx.oppdaterGlobalVersjon(versjon)
                tx.lagre(personRef, objectMapper.writeValueAsString(snapshot), versjon)
            }
        }

    fun utdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        session.transaction { tx ->
            val versjonForSnapshot = tx.finnSnapshotVersjon(fødselsnummer)
            val sisteGjeldendeVersjon = tx.finnGlobalVersjon()

            versjonForSnapshot?.let { it < sisteGjeldendeVersjon } ?: true
        }
    }

    fun hentSnapshotMedMetadata(fødselsnummer: String): Pair<PersoninfoDto, GraphQLPerson>? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                """ SELECT * FROM person AS p
                    INNER JOIN person_info as pi ON pi.id = p.info_ref
                    INNER JOIN snapshot AS s ON s.person_ref = p.id
                WHERE p.fodselsnummer = :fnr;
            """
            session.run(
                queryOf(
                    statement,
                    mapOf("fnr" to fødselsnummer.toLong())
                ).map { row ->
                    val personinfo = PersoninfoDto(
                        fornavn = row.string("fornavn"),
                        mellomnavn = row.stringOrNull("mellomnavn"),
                        etternavn = row.string("etternavn"),
                        fødselsdato = row.localDateOrNull("fodselsdato"),
                        kjønn = row.stringOrNull("kjonn")?.let(Kjønn::valueOf),
                        adressebeskyttelse = row.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf)
                    )
                    val snapshot = objectMapper.readValue<GraphQLPerson>(row.string("data"))
                    personinfo to snapshot
                }.asSingle
            )
        }

    internal fun finnUtbetaling(fødselsnummer: String, utbetalingId: UUID): GraphQLUtbetaling? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
            SELECT * FROM person AS p
            INNER JOIN snapshot AS ss ON ss.person_ref = p.id
            WHERE p.fodselsnummer = ?;
        """
            session.run(
                queryOf(query, fødselsnummer.toLong()).map { row ->
                    objectMapper.readValue<GraphQLPerson>(row.string("data")).arbeidsgivere.firstNotNullOfOrNull { arbeidsgiver ->
                        arbeidsgiver.generasjoner.firstOrNull()?.perioder?.filterIsInstance<GraphQLBeregnetPeriode>()
                            ?.find { periode ->
                                UUID.fromString(periode.utbetaling.id) == utbetalingId
                            }?.utbetaling
                    }
                }.asSingle
            )
        }

    private fun TransactionalSession.lagre(personRef: Int, snapshot: String, versjon: Int): Int {
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
                        "person_ref" to personRef
                    )
                ).asUpdateAndReturnGeneratedKey
            )
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
}

data class PersoninfoDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?,
    val adressebeskyttelse: Adressebeskyttelse
)

enum class Kjønn { Mann, Kvinne, Ukjent }

enum class Adressebeskyttelse {
    Ugradert,
    Fortrolig,
    StrengtFortrolig,
    StrengtFortroligUtland,
    Ukjent
}

internal fun GraphQLUtbetaling?.utbetalingTilSykmeldt() = this != null && personNettoBelop != 0

internal fun GraphQLUtbetaling?.utbetalingTilArbeidsgiver() = this != null && arbeidsgiverNettoBelop != 0

internal fun GraphQLUtbetaling?.delvisRefusjon() = utbetalingTilSykmeldt() && utbetalingTilArbeidsgiver()