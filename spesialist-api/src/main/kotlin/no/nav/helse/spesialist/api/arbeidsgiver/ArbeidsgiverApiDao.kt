package no.nav.helse.spesialist.api.arbeidsgiver

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsgiverInntekterFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.InntektFraAOrdningen
import no.nav.helse.spesialist.api.objectMapper
import java.time.YearMonth
import javax.sql.DataSource

class ArbeidsgiverApiDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun finnBransjer(orgnummer: String) =
        asSQL(
            """
            SELECT ab.bransjer FROM arbeidsgiver a
            LEFT JOIN arbeidsgiver_bransjer ab on a.bransjer_ref = ab.id
            WHERE a.orgnummer=:orgnummer;
            """.trimIndent(),
            "orgnummer" to orgnummer.toLong(),
        ).singleOrNull { row ->
            row.stringOrNull("bransjer")
                ?.let { objectMapper.readValue<List<String>>(it) }
                ?.filter { it.isNotBlank() }
        } ?: emptyList()

    fun finnNavn(orgnummer: String) =
        asSQL(
            """
            SELECT an.navn FROM arbeidsgiver a
            JOIN arbeidsgiver_navn an ON a.navn_ref = an.id
            WHERE a.orgnummer=:orgnummer;
            """.trimIndent(),
            "orgnummer" to orgnummer.toLong(),
        ).singleOrNull { row -> row.string("navn") }

    fun finnArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) = asSQL(
        """
        SELECT startdato, sluttdato, stillingstittel, stillingsprosent FROM arbeidsforhold
        WHERE arbeidsgiver_ref = (SELECT id FROM arbeidsgiver WHERE orgnummer = :orgnummer)
        AND person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer);
        """.trimIndent(),
        "orgnummer" to organisasjonsnummer.toLong(),
        "fodselsnummer" to fødselsnummer,
    ).list { tilArbeidsforholdApiDto(organisasjonsnummer, it) }

    internal fun finnArbeidsgiverInntekterFraAordningen(
        fødselsnummer: String,
        orgnummer: String,
    ): List<ArbeidsgiverInntekterFraAOrdningen> =
        asSQL(
            """
            SELECT inntekter, skjaeringstidspunkt FROM inntekt
            WHERE person_ref=(SELECT id FROM person p WHERE p.fødselsnummer = :fodselsnummer)
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).list { row ->
            ArbeidsgiverInntekterFraAOrdningen(
                skjaeringstidspunkt = row.string("skjaeringstidspunkt"),
                inntekter =
                    objectMapper.readValue<List<Inntekter>>(row.string("inntekter"))
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

    internal data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
        data class Inntekt(val beløp: Double, val orgnummer: String)
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
