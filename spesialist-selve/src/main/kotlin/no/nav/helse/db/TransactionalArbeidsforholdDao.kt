package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.KomplettArbeidsforholdDto
import org.intellij.lang.annotations.Language

internal class TransactionalArbeidsforholdDao(
    private val session: Session,
) : ArbeidsforholdRepository {
    override fun findArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): List<KomplettArbeidsforholdDto> {
        @Language("PostgreSQL")
        val query = """
            SELECT startdato, sluttdato, stillingstittel, stillingsprosent
            FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
              AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer);
        """
        return session.run(
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

    override fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    ) {
        slettArbeidsforhold(fødselsnummer, organisasjonsnummer)
        arbeidsforhold.forEach { komplettArbeidsforhold ->
            TransactionalArbeidsforholdDao(session).insertArbeidsforhold(komplettArbeidsforhold)
        }
    }

    internal fun insertArbeidsforhold(arbeidsforholdDto: KomplettArbeidsforholdDto) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO arbeidsforhold(person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent)
            VALUES(
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer),
                :startdato, :sluttdato, :stillingstittel, :stillingsprosent
            );
        """
        session.run(
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

    internal fun slettArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        @Language("PostgreSQL")
        val deleteQuery = """
            DELETE FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer)
            AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer);
        """
        session.run(
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
