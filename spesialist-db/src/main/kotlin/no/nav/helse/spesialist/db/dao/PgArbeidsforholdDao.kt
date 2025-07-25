package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.list
import no.nav.helse.spesialist.db.HelseDao.Companion.update

class PgArbeidsforholdDao internal constructor(
    private val session: Session,
) : ArbeidsforholdDao {
    override fun findArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
    ) = asSQL(
        """
        SELECT startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert
        FROM arbeidsforhold
        WHERE person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
          AND arbeidsgiver_identifikator = :arbeidsgiver_identifikator;
        """.trimIndent(),
        "fodselsnummer" to fødselsnummer,
        "arbeidsgiver_identifikator" to arbeidsgiverIdentifikator,
    ).list(session) { row ->
        KomplettArbeidsforholdDto(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = arbeidsgiverIdentifikator,
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
            INSERT INTO arbeidsforhold (person_ref, arbeidsgiver_identifikator, startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert)
            VALUES (
                (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer),
                :arbeidsgiver_identifikator, :startdato, :sluttdato, :stillingstittel, :stillingsprosent, :oppdatert
            );
            """.trimIndent(),
            "fodselsnummer" to arbeidsforholdDto.fødselsnummer,
            "arbeidsgiver_identifikator" to arbeidsforholdDto.organisasjonsnummer,
            "startdato" to arbeidsforholdDto.startdato,
            "sluttdato" to arbeidsforholdDto.sluttdato,
            "stillingstittel" to arbeidsforholdDto.stillingstittel,
            "stillingsprosent" to arbeidsforholdDto.stillingsprosent,
            "oppdatert" to arbeidsforholdDto.oppdatert,
        ).update(session)
    }

    private fun slettArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
    ) {
        asSQL(
            """
            DELETE FROM arbeidsforhold
            WHERE person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
            AND arbeidsgiver_identifikator = :arbeidsgiver_identifikator;
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "arbeidsgiver_identifikator" to arbeidsgiverIdentifikator,
        ).update(session)
    }
}
