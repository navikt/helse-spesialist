package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.HelseDao.Companion.list
import no.nav.helse.db.HelseDao.Companion.update
import no.nav.helse.modell.KomplettArbeidsforholdDto

class PgArbeidsforholdDao internal constructor(
    private val session: Session,
) : ArbeidsforholdDao {
    override fun findArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) = asSQL(
        """
        SELECT startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert
        FROM arbeidsforhold
        WHERE person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
          AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer);
        """.trimIndent(),
        "fodselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
    ).list(session) { row ->
        KomplettArbeidsforholdDto(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            startdato = row.localDate("startdato"),
            sluttdato = row.localDateOrNull("sluttdato"),
            stillingsprosent = row.int("stillingsprosent"),
            stillingstittel = row.string("stillingstittel"),
            oppdatert = row.localDateTime("oppdatert"),
        )
    }

    override fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    ) {
        slettArbeidsforhold(fødselsnummer, organisasjonsnummer)
        arbeidsforhold.forEach(::insertArbeidsforhold)
    }

    private fun insertArbeidsforhold(arbeidsforholdDto: KomplettArbeidsforholdDto) {
        asSQL(
            """
            INSERT INTO arbeidsforhold (person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert)
            VALUES (
                (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer),
                (SELECT id FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer),
                :startdato, :sluttdato, :stillingstittel, :stillingsprosent, :oppdatert
            );
            """.trimIndent(),
            "fodselsnummer" to arbeidsforholdDto.fødselsnummer,
            "organisasjonsnummer" to arbeidsforholdDto.organisasjonsnummer,
            "startdato" to arbeidsforholdDto.startdato,
            "sluttdato" to arbeidsforholdDto.sluttdato,
            "stillingstittel" to arbeidsforholdDto.stillingstittel,
            "stillingsprosent" to arbeidsforholdDto.stillingsprosent,
            "oppdatert" to arbeidsforholdDto.oppdatert,
        ).update(session)
    }

    private fun slettArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        asSQL(
            """
            DELETE FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
            AND arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer);
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
        ).update(session)
    }
}
