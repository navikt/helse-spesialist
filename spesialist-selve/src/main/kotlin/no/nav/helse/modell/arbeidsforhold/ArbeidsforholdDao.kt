package no.nav.helse.modell.arbeidsforhold

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.ArbeidsforholdRepository
import no.nav.helse.modell.KomplettArbeidsforholdDto
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class ArbeidsforholdDao(private val dataSource: DataSource) : ArbeidsforholdRepository {
    override fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { transaction ->
            slettArbeidsforhold(transaction, fødselsnummer, organisasjonsnummer)
            arbeidsforhold.forEach { komplettArbeidsforhold ->
                transaction.insertArbeidsforhold(komplettArbeidsforhold)
            }
        }
    }

    override fun findArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): List<KomplettArbeidsforholdDto> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
            SELECT startdato, sluttdato, stillingstittel, stillingsprosent
            FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
              AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer);
        """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "organisasjonsnummer" to organisasjonsnummer.toLong(),
                    ),
                ).map { row ->
                    KomplettArbeidsforholdDto(
                        fødselsnummer = fødselsnummer,
                        organisasjonsnummer = organisasjonsnummer,
                        startdato = row.localDate("startdato"),
                        sluttdato = row.localDateOrNull("sluttdato"),
                        stillingsprosent = row.int("stillingsprosent"),
                        stillingstittel = row.string("stillingstittel"),
                    )
                }.asList,
            )
        }

    private fun Session.insertArbeidsforhold(arbeidsforholdDto: KomplettArbeidsforholdDto) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO arbeidsforhold(person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent)
            VALUES(
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer),
                :startdato, :sluttdato, :stillingstittel, :stillingsprosent
            );
        """
        run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to arbeidsforholdDto.fødselsnummer.toLong(),
                    "organisasjonsnummer" to arbeidsforholdDto.organisasjonsnummer.toLong(),
                    "startdato" to arbeidsforholdDto.startdato,
                    "sluttdato" to arbeidsforholdDto.sluttdato,
                    "stillingstittel" to arbeidsforholdDto.stillingstittel,
                    "stillingsprosent" to arbeidsforholdDto.stillingsprosent,
                ),
            ).asUpdate,
        )
    }

    private fun slettArbeidsforhold(
        transaction: TransactionalSession,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        @Language("PostgreSQL")
        val deleteQuery = """
            DELETE FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
            AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer);
        """
        transaction.run(
            queryOf(
                deleteQuery,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "organisasjonsnummer" to organisasjonsnummer.toLong(),
                ),
            ).asUpdate,
        )
    }
}
