package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.ArbeidsgiverApiDao.Inntekter
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class PgArbeidsgiverApiDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `finner arbeidsforhold`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettArbeidsforhold()

        val arbeidsforhold = arbeidsgiverApiDao.finnArbeidsforhold(
            fødselsnummer = FNR,
            arbeidsgiverIdentifikator = ORGNUMMER
        )

        assertEquals(1, arbeidsforhold.size)
        assertEquals(ARBEIDSFORHOLD.start, arbeidsforhold.first().startdato)
        assertEquals(ARBEIDSFORHOLD.slutt, arbeidsforhold.first().sluttdato)
        assertEquals(ARBEIDSFORHOLD.tittel, arbeidsforhold.first().stillingstittel)
        assertEquals(ARBEIDSFORHOLD.prosent, arbeidsforhold.first().stillingsprosent)
    }

    @Test
    fun `Finn inntekter fra aordningen for arbeidsgiveren i 3 foregående måneder for alle skjæringstidspunkt`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettInntekt(
            skjæringstidspunkt = LocalDate.parse("2020-01-01"),
            inntekter = listOf(
                Inntekter(
                    årMåned = YearMonth.of(2019, 12),
                    inntektsliste = listOf(
                        Inntekter.Inntekt(beløp = 19000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = "123456789"),
                    )
                ),
                Inntekter(
                    årMåned = YearMonth.of(2019, 11),
                    inntektsliste = listOf(
                        Inntekter.Inntekt(beløp = 21000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = "123456789"),
                    )
                ),
                Inntekter(
                    årMåned = YearMonth.of(2019, 10),
                    inntektsliste = listOf(
                        Inntekter.Inntekt(beløp = 21000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 2000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = "123456789"),
                    )
                )
            )
        )
        opprettInntekt(
            skjæringstidspunkt = LocalDate.parse("2022-11-11"),
            inntekter = listOf(
                Inntekter(
                    årMåned = YearMonth.of(2022, 10),
                    inntektsliste = listOf(
                        Inntekter.Inntekt(beløp = 20000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = "123456789"),
                    )
                ),
                Inntekter(
                    årMåned = YearMonth.of(2022, 9),
                    inntektsliste = listOf(
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = "123456789"),
                    )
                ),
                Inntekter(
                    årMåned = YearMonth.of(2022, 8),
                    inntektsliste = listOf(
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 2000.0, orgnummer = ORGNUMMER),
                        Inntekter.Inntekt(beløp = 22000.0, orgnummer = "123456789"),
                    )
                )
            )
        )

        val arbeidsgiverInntekterFraAordningen = arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer = FNR,
            orgnummer = ORGNUMMER
        )

        assertEquals(2, arbeidsgiverInntekterFraAordningen.size)
        assertEquals(2, arbeidsgiverInntekterFraAordningen.size)
        assertEquals(3, arbeidsgiverInntekterFraAordningen.first().inntekter.size)
        assertEquals(3, arbeidsgiverInntekterFraAordningen.last().inntekter.size)
        assertEquals(19000.0, arbeidsgiverInntekterFraAordningen.first().inntekter.first().sum)
        assertEquals(23000.0, arbeidsgiverInntekterFraAordningen.first().inntekter.last().sum)
        assertEquals(20000.0, arbeidsgiverInntekterFraAordningen.last().inntekter.first().sum)
        assertEquals(24000.0, arbeidsgiverInntekterFraAordningen.last().inntekter.last().sum)

        val inntektFraAordningenFeilOrgnummer = arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer = FNR,
            orgnummer = "123123123"
        )

        assertEquals(0, inntektFraAordningenFeilOrgnummer.size)
    }

    @Test
    fun `Returnerer tomt array om arbeidsgiver ikke har noen inntekter lagret`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val inntektFraAordningen = arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer = FNR,
            orgnummer = ORGNUMMER
        )

        assertEquals(0, inntektFraAordningen.size)
        assertTrue(inntektFraAordningen.isEmpty())
    }

    private fun opprettInntekt(
        fødselsnummer: String = FNR,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = dbQuery.update(
        """
        with person_id as (select id from person where fødselsnummer = :foedselsnummer)
        insert into inntekt (person_ref, skjaeringstidspunkt, inntekter)
        select id, :skjaeringstidspunkt, :inntekter::json
        from person_id
        """.trimIndent(),
        "foedselsnummer" to fødselsnummer,
        "skjaeringstidspunkt" to skjæringstidspunkt,
        "inntekter" to objectMapper.writeValueAsString(inntekter),
    )

}
