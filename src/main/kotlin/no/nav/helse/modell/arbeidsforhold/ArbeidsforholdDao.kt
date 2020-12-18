package no.nav.helse.modell.arbeidsforhold

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class ArbeidsforholdDao(private val dataSource: DataSource) {
    internal fun insertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        startdato: LocalDate,
        sluttdato: LocalDate?,
        stillingstittel: String,
        stillingsprosent: Int
    ): Long = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO arbeidsforhold(person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent)
            VALUES(
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer),
                :startdato, :sluttdato, :stillingstittel, :stillingsprosent
            );
        """
        requireNotNull(
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "organisasjonsnummer" to organisasjonsnummer.toLong(),
                        "startdato" to startdato,
                        "sluttdato" to sluttdato,
                        "stillingstittel" to stillingstittel,
                        "stillingsprosent" to stillingsprosent
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    fun findArbeidsforhold(fødselsnummer: String, organisasjonsnummer: String): List<ArbeidsforholdDto> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                SELECT * FROM arbeidsforhold
                WHERE arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer)
                    AND person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
            """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "organisasjonsnummer" to organisasjonsnummer.toLong()
                    )
                ).map { row ->
                    ArbeidsforholdDto(
                        personId = row.long("person_ref"),
                        arbeidsgiverId = row.long("arbeidsgiver_ref"),
                        startdato = row.localDate("startdato"),
                        sluttdato = row.localDateOrNull("sluttdato"),
                        stillingsprosent = row.int("stillingsprosent"),
                        stillingstittel = row.string("stillingstittel")
                    )
                }.asList
            )
        }

    fun oppdaterArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        startdato: LocalDate,
        sluttdato: LocalDate?,
        stillingstittel: String,
        stillingsprosent: Int
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            UPDATE arbeidsforhold
            SET startdato=:startdato,
                sluttdato=:sluttdato,
                stillingstittel=:stillingstittel,
                stillingsprosent=:stillingsprosent
            WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
              AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer);
        """
        requireNotNull(
            session.run(
                queryOf(
                    query, mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "organisasjonsnummer" to organisasjonsnummer.toLong(),
                        "startdato" to startdato,
                        "sluttdato" to sluttdato,
                        "stillingstittel" to stillingstittel,
                        "stillingsprosent" to stillingsprosent
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    internal fun findArbeidsforholdSistOppdatert(
        fødselsnummer: String,
        organisasjonsnummer: String
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT oppdatert
            FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
              AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer);
        """
        requireNotNull(
            session.run(
                queryOf(
                    query, mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "organisasjonsnummer" to organisasjonsnummer.toLong()
                    )
                )
                    .map { row -> row.sqlDate("oppdatert").toLocalDate() }
                    .asSingle
            )
        )
    }
}
