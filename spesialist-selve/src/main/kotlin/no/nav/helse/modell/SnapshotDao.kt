package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.graphql.enums.Utbetalingtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
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

internal fun GraphQLUtbetaling?.utbetalingTilSykmeldt() = this != null && personNettoBelop != 0

internal fun GraphQLUtbetaling?.utbetalingTilArbeidsgiver() = this != null && arbeidsgiverNettoBelop != 0

internal fun GraphQLUtbetaling?.delvisRefusjon() = utbetalingTilSykmeldt() && utbetalingTilArbeidsgiver()

internal fun GraphQLUtbetaling?.erRevurdering() = this?.typeEnum == Utbetalingtype.REVURDERING
