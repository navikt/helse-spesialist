package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.til
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class GraderingsperioderTest {
    @Test
    fun `legg til ny graderingsperiode`() {
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val graderingsperiode = Graderingsperiode.ny(
            fom = 1 jan 2018,
            tom = 2 jan 2018,
            dager = listOf(1 jan 2018, 2 jan 2018),
            periodebeløp = 10000.0
        )
        graderingsperioder.leggTilGraderingsperiode(graderingsperiode)
        val events = graderingsperioder.konsumerDomenehendelser()
        assertEquals(1, events.size)
        assertEquals(1, events[0].graderingsdifferanse.nyeEllerEndredeInntekter.size)
        assertEquals(0, events[0].graderingsdifferanse.fjernedeInntekter.size)
    }

    @Test
    fun `kan ikke legge til periode som overlapper med annen periode`() {
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val graderingsperiode = Graderingsperiode.ny(
            fom = 1 jan 2018,
            tom = 2 jan 2018,
            dager = listOf(1 jan 2018, 2 jan 2018),
            periodebeløp = 10000.0
        )
        graderingsperioder.leggTilGraderingsperiode(graderingsperiode)
        assertThrows<IllegalStateException> {
            graderingsperioder.leggTilGraderingsperiode(graderingsperiode)
        }
    }

    @Test
    fun `kan endre graderingsperiode`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val graderingsperiodeV1 = Graderingsperiode.ny(
            fom = 1 jan 2018,
            tom = 2 jan 2018,
            dager = listOf(1 jan 2018, 2 jan 2018),
            periodebeløp = 10000.0
        )
        graderingsperioder.leggTilGraderingsperiode(graderingsperiodeV1)
        graderingsperioder.konsumerDomenehendelser() // flush

        // when
        val graderingsperiodeV2 = Graderingsperiode.ny(
            fom = 1 jan 2018,
            tom = 3 jan 2018,
            dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018),
            periodebeløp = 10000.0
        )
        graderingsperioder.endreGraderingsperiode(gammel = graderingsperiodeV1, ny = graderingsperiodeV2)
        val hendelser = graderingsperioder.konsumerDomenehendelser()

        // then
        assertEquals(1, hendelser.size)
        assertEquals(1, hendelser[0].graderingsdifferanse.nyeEllerEndredeInntekter.size)
        assertEquals(0, hendelser[0].graderingsdifferanse.fjernedeInntekter.size)
    }

    @Test
    fun `kan ikke endre graderingsperiode til periode som overlapper med annen periode`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val gammelGradering = 10000.0 fordeltPå (1 jan 2018 til 2 jan 2018)
        graderingsperioder.perioder(
            gammelGradering,
            10000.0 fordeltPå (4 jan 2018 til 5 jan 2018),
        )

        // when, then
        assertThrows<IllegalStateException> {
            graderingsperioder.endreGraderingsperiode(
                gammelGradering,
                10000.0 fordeltPå (2 jan 2018 til 4 jan 2018)
            )
        }
    }

    @Test
    fun `kan ikke endre graderingsperiode som ikke finnes`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val graderingSomIkkeFinnes = 10000.0 fordeltPå (1 jan 2018 til 2 jan 2018)

        // when, then
        assertThrows<IllegalStateException> {
            graderingsperioder.endreGraderingsperiode(
                graderingSomIkkeFinnes,
                10000.0 fordeltPå (2 jan 2018 til 4 jan 2018)
            )
        }
    }

    @Test
    fun `kan fjerne graderingsperiode`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val graderingsperiode = 10000.0 fordeltPå (1 jan 2018 til 2 jan 2018)

        // when
        graderingsperioder.leggTilGraderingsperiode(graderingsperiode)
        graderingsperioder.konsumerDomenehendelser() // flush

        graderingsperioder.fjernGraderingsperiode(graderingsperiode)

        // then
        val hendelser = graderingsperioder.konsumerDomenehendelser()
        assertEquals(1, hendelser.size)
        assertEquals(0, hendelser[0].graderingsdifferanse.nyeEllerEndredeInntekter.size)
        assertEquals(1, hendelser[0].graderingsdifferanse.fjernedeInntekter.size)
    }

    @Test
    fun `kan ikke fjerne graderingsperiode som ikke finnes`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val graderingsperiode = 10000.0 fordeltPå (1 jan 2018 til 2 jan 2018)

        // when, then
        assertThrows<IllegalStateException> {
            graderingsperioder.fjernGraderingsperiode(graderingsperiode)
        }
    }

    @Test
    fun `kan i teorien legge til, fjerne og endre graderingsperioder i en go`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val vilBliFjernet = 10000.0 fordeltPå (1 jan 2018 til 5 jan 2018)
        val vilBliLagtTil = 10000.0 fordeltPå (6 jan 2018 til 10 jan 2018)
        val vilBliEndret = 10000.0 fordeltPå (11 jan 2018 til 15 jan 2018)
        graderingsperioder.perioder(vilBliFjernet, vilBliEndret)
        graderingsperioder.konsumerDomenehendelser() // flush

        // when
        graderingsperioder.fjernGraderingsperiode(vilBliFjernet)
        graderingsperioder.leggTilGraderingsperiode(vilBliLagtTil)
        graderingsperioder.endreGraderingsperiode(vilBliEndret, vilBliEndret.copy(periodebeløp = 15000.0.toBigDecimal()))

        val hendelser = graderingsperioder.konsumerDomenehendelser()
        // then
        assertEquals(3, hendelser.size)
        assertEquals(1, hendelser[0].graderingsdifferanse.fjernedeInntekter.size)
        assertEquals(0, hendelser[0].graderingsdifferanse.nyeEllerEndredeInntekter.size)

        assertEquals(0, hendelser[1].graderingsdifferanse.fjernedeInntekter.size)
        assertEquals(1, hendelser[1].graderingsdifferanse.nyeEllerEndredeInntekter.size)

        assertEquals(1, hendelser[2].graderingsdifferanse.nyeEllerEndredeInntekter.size)
        assertEquals(0, hendelser[2].graderingsdifferanse.fjernedeInntekter.size)
    }

    private fun Graderingsperioder.perioder(vararg graderingsperiode: Graderingsperiode) {
        graderingsperiode.forEach { this.leggTilGraderingsperiode(it) }
    }

    private infix fun Double.fordeltPå(periode: Periode): Graderingsperiode {
        return Graderingsperiode.ny(
            fom = periode.fom,
            tom = periode.tom,
            dager = periode.datoer(),
            periodebeløp = this
        )
    }

    private infix fun LocalDate.til(other: Int) = PeriodBuilder(this, other)

    class PeriodBuilder(private val fom: LocalDate, private val int: Int) {
        infix fun jan(target: Int): Periode = fom til LocalDate.of(target, Month.JANUARY, int)
    }
}
