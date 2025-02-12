package no.nav.helse.db.api

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.api.ArbeidsgiverApiDao.ArbeidsgiverInntekterFraAOrdningen
import no.nav.helse.db.api.ArbeidsgiverApiDao.InntektFraAOrdningen
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto
import no.nav.helse.spesialist.db.objectMapper
import javax.sql.DataSource

class PgArbeidsgiverApiDao internal constructor(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource), ArbeidsgiverApiDao {
    override fun finnBransjer(organisasjonsnummer: String) =
        asSQL(
            """
            SELECT ab.bransjer FROM arbeidsgiver a
            LEFT JOIN arbeidsgiver_bransjer ab on a.bransjer_ref = ab.id
            WHERE a.organisasjonsnummer = :organisasjonsnummer;
            """.trimIndent(),
            "organisasjonsnummer" to organisasjonsnummer,
        ).singleOrNull { row ->
            row.stringOrNull("bransjer")
                ?.let { objectMapper.readValue<List<String>>(it) }
                ?.filter { it.isNotBlank() }
        } ?: emptyList()

    override fun finnNavn(organisasjonsnummer: String) =
        asSQL(
            """
            SELECT an.navn FROM arbeidsgiver a
            JOIN arbeidsgiver_navn an ON a.navn_ref = an.id
            WHERE a.organisasjonsnummer=:organisasjonsnummer;
            """.trimIndent(),
            "organisasjonsnummer" to organisasjonsnummer,
        ).singleOrNull { row -> row.string("navn") }

    override fun finnArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) = asSQL(
        """
        SELECT startdato, sluttdato, stillingstittel, stillingsprosent FROM arbeidsforhold
        WHERE arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer)
        AND person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer);
        """.trimIndent(),
        "organisasjonsnummer" to organisasjonsnummer,
        "fodselsnummer" to fødselsnummer,
    ).list { tilArbeidsforholdApiDto(organisasjonsnummer, it) }

    override fun finnArbeidsgiverInntekterFraAordningen(
        fødselsnummer: String,
        orgnummer: String,
    ): List<ArbeidsgiverInntekterFraAOrdningen> =
        asSQL(
            """
            SELECT inntekter, skjaeringstidspunkt FROM inntekt
            WHERE person_ref = (SELECT id FROM person p WHERE p.fødselsnummer = :fodselsnummer)
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).list { row ->
            ArbeidsgiverInntekterFraAOrdningen(
                skjaeringstidspunkt = row.string("skjaeringstidspunkt"),
                inntekter =
                    objectMapper.readValue<List<ArbeidsgiverApiDao.Inntekter>>(row.string("inntekter"))
                        .mapNotNull { inntekter ->
                            inntekter.inntektsliste.filter { it.orgnummer == orgnummer }.takeUnless { it.isEmpty() }
                                ?.let { inntekter.copy(inntektsliste = it) }
                        }.map { inntekter ->
                            InntektFraAOrdningen(
                                maned = inntekter.årMåned,
                                sum = inntekter.inntektsliste.sumOf { it.beløp },
                            )
                        },
            ).takeIf { it.inntekter.isNotEmpty() }
        }

    private fun tilArbeidsforholdApiDto(
        organisasjonsnummer: String,
        row: Row,
    ) = ArbeidsforholdApiDto(
        organisasjonsnummer = organisasjonsnummer,
        stillingstittel = row.string("stillingstittel"),
        stillingsprosent = row.int("stillingsprosent"),
        startdato = row.localDate("startdato"),
        sluttdato = row.localDateOrNull("sluttdato"),
    )
}
