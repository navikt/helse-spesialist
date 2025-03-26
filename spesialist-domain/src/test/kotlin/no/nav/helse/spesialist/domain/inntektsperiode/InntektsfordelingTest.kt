package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.til
import no.nav.helse.spesialist.domain.inntektsperiode.Inntektsendringer.PeriodeMedBeløp
import no.nav.helse.spesialist.domain.testfixtures.jan
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class InntektsfordelingTest {
    @Test
    fun `lagt til en dag`() {
        val gammelFordeling = 1000.0 fordeltPå (1 jan 2018)
        val nyFordeling = 2000.0 fordeltPå (1 jan 2018 til 2 jan 2018)
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(2 jan 2018 til 2 jan 2018, 1000.0), actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `lagt til en dag og endret beløp`() {
        val gammelFordeling = 1000.0 fordeltPå (1 jan 2018)
        val nyFordeling = 3000.0 fordeltPå (1 jan 2018 til 2 jan 2018)
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(1 jan 2018 til 2 jan 2018, 1500.0), actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `lagt til to dager`() {
        val gammelFordeling = 1000.0 fordeltPå (1 jan 2018)
        val nyFordeling = 3000.0 fordeltPå (1 jan 2018 til 3 jan 2018)
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(2 jan 2018 til 3 jan 2018, 1000.0), actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `fjernet to dager`() {
        val gammelFordeling = 3000.0 fordeltPå (1 jan 2018 til 3 jan 2018)
        val nyFordeling = 1000.0 fordeltPå (1 jan 2018)
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(1, differanse.fjernedeInntekter.size)
        assertEquals(0, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = 2 jan 2018 til 3 jan 2018,
            actual = differanse.fjernedeInntekter[0]
        )
    }

    @Test
    fun `ta bort alle dager`() {
        val gammelFordeling = 10000.0 fordeltPå listOf(
            1 jan 2018 til 5 jan 2018,
            7 jan 2018 til 11 jan 2018
        )
        val nyFordeling = ingenFordeling
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(2, differanse.fjernedeInntekter.size)
        assertEquals(0, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = listOf(
                1 jan 2018 til 5 jan 2018,
                7 jan 2018 til 11 jan 2018,
            ),
            actual = differanse.fjernedeInntekter
        )
    }

    @Test
    fun `legg til alle dager`() {
        val gammelFordeling = ingenFordeling
        val nyFordeling = 31000.0 fordeltPå (1 jan 2018 til 31 jan 2018)
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = listOf(PeriodeMedBeløp(1 jan 2018 til 31 jan 2018, 1000.0)),
            actual = differanse.nyeEllerEndredeInntekter
        )
    }

    @Test
    fun potpourri() {
        val gammelFordeling = 17000.0 fordeltPå listOf(
            1 jan 2018 til 5 jan 2018,
            7 jan 2018 til 7 jan 2018,
            9 jan 2018 til 14 jan 2018,
            16 jan 2018 til 16 jan 2018,
        )
        val nyFordeling = 18000.0 fordeltPå listOf(
            1 jan 2018 til 4 jan 2018,
            7 jan 2018 til 7 jan 2018,
            9 jan 2018 til 10 jan 2018,
            13 jan 2018 til 14 jan 2018
        )
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(3, differanse.fjernedeInntekter.size)
        assertEquals(4, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = listOf(
                PeriodeMedBeløp(1 jan 2018 til 4 jan 2018, 2000.0),
                PeriodeMedBeløp(7 jan 2018 til 7 jan 2018, 2000.0),
                PeriodeMedBeløp(9 jan 2018 til 10 jan 2018, 2000.0),
                PeriodeMedBeløp(13 jan 2018 til 14 jan 2018, 2000.0),
            ),
            actual = differanse.nyeEllerEndredeInntekter
        )
        assertEquals(
            expected = listOf(
                5 jan 2018 til 5 jan 2018,
                11 jan 2018 til 12 jan 2018,
                16 jan 2018 til 16 jan 2018,
            ),
            actual = differanse.fjernedeInntekter
        )
    }

    private infix fun Double.fordeltPå(perioder: List<Periode>): Inntektsfordeling {
        return Inntektsfordeling.ny(
            periodebeløp = this,
            dager = perioder.flatMap { it.datoer() }
        )
    }

    private val ingenFordeling = Inntektsfordeling.ny(0.0, emptyList())

    private infix fun Double.fordeltPå(periode: Periode): Inntektsfordeling = this.fordeltPå(listOf(periode))
    private infix fun Double.fordeltPå(dato: LocalDate): Inntektsfordeling = this.fordeltPå(listOf(dato til dato))

    private infix fun LocalDate.til(other: Int) = PeriodBuilder(this, other)

    class PeriodBuilder(private val fom: LocalDate, private val int: Int) {
        infix fun jan(target: Int): Periode = fom til LocalDate.of(target, Month.JANUARY, int)
    }
}
