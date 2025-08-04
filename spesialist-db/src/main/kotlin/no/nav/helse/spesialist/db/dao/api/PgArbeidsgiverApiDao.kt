package no.nav.helse.spesialist.db.dao.api

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.ArbeidsgiverApiDao.ArbeidsgiverInntekterFraAOrdningen
import no.nav.helse.db.api.ArbeidsgiverApiDao.InntektFraAOrdningen
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import javax.sql.DataSource

class PgArbeidsgiverApiDao internal constructor(
    dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource),
    ArbeidsgiverApiDao {
    override fun finnArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
    ) = asSQL(
        """
        SELECT startdato, sluttdato, stillingstittel, stillingsprosent FROM arbeidsforhold
        WHERE arbeidsgiver_identifikator = :arbeidsgiver_identifikator
        AND person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer);
        """.trimIndent(),
        "arbeidsgiver_identifikator" to arbeidsgiverIdentifikator,
        "fodselsnummer" to fødselsnummer,
    ).list { tilArbeidsforholdApiDto(arbeidsgiverIdentifikator, it) }

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
                    objectMapper
                        .readValue<List<ArbeidsgiverApiDao.Inntekter>>(row.string("inntekter"))
                        .mapNotNull { inntekter ->
                            inntekter.inntektsliste
                                .filter { it.orgnummer == orgnummer }
                                .takeUnless { it.isEmpty() }
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
