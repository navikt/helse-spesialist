package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.til
import no.nav.helse.spesialist.domain.inntektsperiode.Inntektsendringer.PeriodeMedBeløp
import no.nav.helse.spesialist.domain.inntektsperiode.Inntektsendringer.PeriodeUtenBeløp
import no.nav.helse.spesialist.domain.testfixtures.jan
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class InntektsfordelingTest {
    @Test
    fun `lagt til en dag`() {
        val gammelFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018))
        val nyFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018 til 2 jan 2018))
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(2 jan 2018, 2 jan 2018, 2000.0), actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `lagt til en dag og endret beløp`() {
        val gammelFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018))
        val nyFordeling = nyFordeling(3000.0 fordeltPå (1 jan 2018 til 2 jan 2018))
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(1 jan 2018, 2 jan 2018, 3000.0), actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `lagt til to dager`() {
        val gammelFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018))
        val nyFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018 til 3 jan 2018))
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(2 jan 2018, 3 jan 2018, 2000.0), actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `fjernet to dager`() {
        val gammelFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018 til 3 jan 2018))
        val nyFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018))
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(1, differanse.fjernedeInntekter.size)
        assertEquals(0, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeUtenBeløp(2 jan 2018, 3 jan 2018),
            actual = differanse.fjernedeInntekter[0]
        )
    }

    @Test
    fun `ta bort alle dager`() {
        val fordeling2 = nyFordeling(2000.0 fordeltPå (1 jan 2018 til 5 jan 2018), 3000.0 fordeltPå (7 jan 2018 til 11 jan 2018))
        val fordeling3 = nyFordeling(emptyList())
        val differanse = fordeling3 diff fordeling2
        assertEquals(2, differanse.fjernedeInntekter.size)
        assertEquals(0, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = listOf(
                PeriodeUtenBeløp(1 jan 2018, 5 jan 2018),
                PeriodeUtenBeløp(7 jan 2018, 11 jan 2018),
            ),
            actual = differanse.fjernedeInntekter
        )
    }

    @Test
    fun `legg til alle dager`() {
        val gammelFordeling = nyFordeling(emptyList())
        val nyFordeling = nyFordeling(2000.0 fordeltPå (1 jan 2018 til 31 jan 2018))
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = listOf(PeriodeMedBeløp(1 jan 2018, 31 jan 2018, 2000.0)),
            actual = differanse.nyeEllerEndredeInntekter
        )
    }

    @Test
    fun potpourri() {
        val gammelFordeling = nyFordeling(
            2000.0 fordeltPå (1 jan 2018 til 5 jan 2018),
            3000.0 fordeltPå (7 jan 2018),
            4000.0 fordeltPå (9 jan 2018 til 14 jan 2018),
            2000.0 fordeltPå (16 jan 2018)
        )
        val nyFordeling = nyFordeling(
            2000.0 fordeltPå (1 jan 2018 til 4 jan 2018),
            4000.0 fordeltPå (7 jan 2018),
            4000.0 fordeltPå (9 jan 2018 til 10 jan 2018),
            3000.0 fordeltPå (13 jan 2018 til 14 jan 2018)
        )
        val differanse = nyFordeling diff gammelFordeling
        assertEquals(3, differanse.fjernedeInntekter.size)
        assertEquals(2, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = listOf(
                PeriodeMedBeløp(7 jan 2018, 7 jan 2018, 4000.0),
                PeriodeMedBeløp(13 jan 2018, 14 jan 2018, 3000.0),
            ),
            actual = differanse.nyeEllerEndredeInntekter
        )
        assertEquals(
            expected = listOf(
                PeriodeUtenBeløp(5 jan 2018, 5 jan 2018),
                PeriodeUtenBeløp(11 jan 2018, 12 jan 2018),
                PeriodeUtenBeløp(16 jan 2018, 16 jan 2018),
            ),
            actual = differanse.fjernedeInntekter
        )
    }

    private fun nyFordeling(vararg inntektsdager: List<Inntektsdag>) = Inntektsfordeling.ny(inntektsdager.flatMap { it })

    private infix fun Double.fordeltPå(dato: LocalDate): List<Inntektsdag> {
        return listOf(Inntektsdag(dato, this))
    }

     private infix fun Double.fordeltPå(periode: Periode): List<Inntektsdag> {
        return periode.datoer().map { Inntektsdag(it, this) }
    }

    private infix fun LocalDate.til(other: Int) = DateBuilder(this, other)

    class DateBuilder(private val fom: LocalDate, private val int: Int) {
        infix fun jan(target: Int): Periode = fom til LocalDate.of(target, Month.JANUARY, int)
    }
}
