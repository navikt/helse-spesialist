package no.nav.helse.db.api

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.db.api.ArbeidsgiverApiDao.Inntekter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class PgArbeidsgiverApiDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner bransjer`() {
        val bransjer = listOf("bransje 1", "bransje 2")
        opprettArbeidsgiver(bransjer = bransjer)
        assertEquals(bransjer, arbeidsgiverApiDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `finner bransjer når det ikke er noen bransjer`() {
        opprettArbeidsgiver(bransjer = emptyList())
        arbeidsgiverApiDao.finnBransjer(ORGNUMMER).let {
            assertTrue(it.isEmpty()) { "Forventet at $it skulle være tom liste"}
        }
    }

    @Test
    fun `finner navn`() {
        opprettArbeidsgiver()
        assertEquals(ORGNAVN, arbeidsgiverApiDao.finnNavn(ORGNUMMER))
    }

    @Test
    fun `finner arbeidsforhold`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettArbeidsforhold()

        val arbeidsforhold = arbeidsgiverApiDao.finnArbeidsforhold(FNR, ORGNUMMER)

        assertEquals(1, arbeidsforhold.size)
        assertEquals(ARBEIDSFORHOLD.start, arbeidsforhold.first().startdato)
        assertEquals(ARBEIDSFORHOLD.slutt, arbeidsforhold.first().sluttdato)
        assertEquals(ARBEIDSFORHOLD.tittel, arbeidsforhold.first().stillingstittel)
        assertEquals(ARBEIDSFORHOLD.prosent, arbeidsforhold.first().stillingsprosent)
    }

    @Test
    fun `Finn inntekter fra aordningen for arbeidsgiveren i 3 foregående måneder for alle skjæringstidspunkt`() {
        val (personId) = opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettInntekt(
            personId,
            LocalDate.parse("2020-01-01"),
            listOf(
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
            personId,
            LocalDate.parse("2022-11-11"),
            listOf(
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
}
