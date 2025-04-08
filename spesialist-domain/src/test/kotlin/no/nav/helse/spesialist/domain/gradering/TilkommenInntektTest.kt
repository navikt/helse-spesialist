package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test

class TilkommenInntektTest {

    @Test
    fun `kan ikke legge til periode som overlapper med annen periode`() {
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            dager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000.0"),
            fødselsnummer = fødselsnummer,
            saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        assertThrows<IllegalStateException> {
        TilkommenInntekt.validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(15 jan 2018, 31 jan 2018, organisasjonsnummer, listOf(tilkommenInntekt))
        }
    }

    /*@Test
    fun `kan endre graderingsperiode`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val tilkommenInntektV1 = TilkommenInntekt.ny(
            fom = 1 jan 2018,
            tom = 2 jan 2018,
            dager = listOf(1 jan 2018, 2 jan 2018),
            periodebeløp = 10000.0
        )
        graderingsperioder.leggTilGraderingsperiode(tilkommenInntektV1)
        graderingsperioder.konsumerDomenehendelser() // flush

        // when
        val tilkommenInntektV2 = TilkommenInntekt.ny(
            fom = 1 jan 2018,
            tom = 3 jan 2018,
            dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018),
            periodebeløp = 10000.0
        )
        graderingsperioder.endreGraderingsperiode(gammel = tilkommenInntektV1, ny = tilkommenInntektV2)
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
        val perioder = (1 jan 2018 til 2 jan 2018)
        val gammelGradering = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )
        val perioder1 = (4 jan 2018 til 5 jan 2018)
        graderingsperioder.perioder(
     gammelGradering,
     TilkommenInntekt.ny(
         fom = perioder1.first<Periode>().fom,
         tom = perioder1.last<Periode>().tom,
         dager = perioder1.flatMap<Periode, LocalDate> { it.datoer() },
         periodebeløp = 10000.0,
     ),
 )

        // when, then
        assertThrows<IllegalStateException> {
            val perioder2 = (2 jan 2018 til 4 jan 2018)
            graderingsperioder.endreGraderingsperiode(
     gammelGradering,
     TilkommenInntekt.ny(
         fom = perioder2.first<Periode>().fom,
         tom = perioder2.last<Periode>().tom,
         dager = perioder2.flatMap<Periode, LocalDate> { it.datoer() },
         periodebeløp = 10000.0,
     )
 )
        }
    }

    @Test
    fun `kan ikke endre graderingsperiode som ikke finnes`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val perioder = (1 jan 2018 til 2 jan 2018)
        val graderingSomIkkeFinnes = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )

        // when, then
        assertThrows<IllegalStateException> {
            val perioder1 = (2 jan 2018 til 4 jan 2018)
            graderingsperioder.endreGraderingsperiode(
     graderingSomIkkeFinnes,
     TilkommenInntekt.ny(
         fom = perioder1.first<Periode>().fom,
         tom = perioder1.last<Periode>().tom,
         dager = perioder1.flatMap<Periode, LocalDate> { it.datoer() },
         periodebeløp = 10000.0,
     )
 )
        }
    }

    @Test
    fun `kan fjerne graderingsperiode`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val perioder = (1 jan 2018 til 2 jan 2018)
        val graderingsperiode = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )

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
        val perioder = (1 jan 2018 til 2 jan 2018)
        val graderingsperiode = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )

        // when, then
        assertThrows<IllegalStateException> {
            graderingsperioder.fjernGraderingsperiode(graderingsperiode)
        }
    }

    @Test
    fun `kan i teorien legge til, fjerne og endre graderingsperioder i en go`() {
        // given
        val graderingsperioder = Graderingsperioder.ny(lagFødselsnummer(), lagOrganisasjonsnummer())
        val perioder = (1 jan 2018 til 5 jan 2018)
        val vilBliFjernet = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )
        val perioder1 = (6 jan 2018 til 10 jan 2018)
        val vilBliLagtTil = TilkommenInntekt.ny(
            fom = perioder1.first<Periode>().fom,
            tom = perioder1.last<Periode>().tom,
            dager = perioder1.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )
        val perioder2 = (11 jan 2018 til 15 jan 2018)
        val vilBliEndret = TilkommenInntekt.ny(
            fom = perioder2.first<Periode>().fom,
            tom = perioder2.last<Periode>().tom,
            dager = perioder2.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 10000.0,
        )
        graderingsperioder.perioder(vilBliFjernet, vilBliEndret)
        graderingsperioder.konsumerDomenehendelser() // flush

        // when
        graderingsperioder.fjernGraderingsperiode(vilBliFjernet)
        graderingsperioder.leggTilGraderingsperiode(vilBliLagtTil)
        graderingsperioder.endreGraderingsperiode(
            vilBliEndret,
            vilBliEndret.copy(periodebeløp = 15000.0.toBigDecimal())
        )

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

    private fun Graderingsperioder.perioder(vararg tilkommenInntekt: TilkommenInntekt) {
        tilkommenInntekt.forEach { this.leggTilGraderingsperiode(it) }
    }

    private infix fun LocalDate.til(other: Int) = PeriodBuilder(this, other)

    class PeriodBuilder(private val fom: LocalDate, private val int: Int) {
        infix fun jan(target: Int): Periode = fom tilOgMed LocalDate.of(target, Month.JANUARY, int)
    }

    @Test
    fun `lagt til en dag`() {
        val dato = (1 jan 2018)
        val perioder = listOf((dato tilOgMed dato))
        val gammelFordeling = TilkommenInntekt.ny(
            fom = perioder.first().fom,
            tom = perioder.last().tom,
            dager = perioder.flatMap { it.datoer() },
            periodebeløp = 1000.0,
        )
        val perioder1 = (1 jan 2018 til 2 jan 2018)
        val nyFordeling = TilkommenInntekt.ny(
            fom = perioder1.first<Periode>().fom,
            tom = perioder1.last<Periode>().tom,
            dager = perioder1.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 2000.0,
        )
        val differanse = nyFordeling differanseFra gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(2 jan 2018 til 2 jan 2018, 1000.0),
            actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `lagt til en dag og endret beløp`() {
        val dato = (1 jan 2018)
        val perioder = listOf((dato tilOgMed dato))
        val gammelFordeling = TilkommenInntekt.ny(
            fom = perioder.first().fom,
            tom = perioder.last().tom,
            dager = perioder.flatMap { it.datoer() },
            periodebeløp = 1000.0,
        )
        val perioder1 = (1 jan 2018 til 2 jan 2018)
        val nyFordeling = TilkommenInntekt.ny(
            fom = perioder1.first<Periode>().fom,
            tom = perioder1.last<Periode>().tom,
            dager = perioder1.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 3000.0,
        )
        val differanse = nyFordeling differanseFra gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(1 jan 2018 til 2 jan 2018, 1500.0),
            actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `lagt til to dager`() {
        val dato = (1 jan 2018)
        val perioder = listOf((dato tilOgMed dato))
        val gammelFordeling = TilkommenInntekt.ny(
            fom = perioder.first().fom,
            tom = perioder.last().tom,
            dager = perioder.flatMap { it.datoer() },
            periodebeløp = 1000.0,
        )
        val perioder1 = (1 jan 2018 til 3 jan 2018)
        val nyFordeling = TilkommenInntekt.ny(
            fom = perioder1.first<Periode>().fom,
            tom = perioder1.last<Periode>().tom,
            dager = perioder1.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 3000.0,
        )
        val differanse = nyFordeling differanseFra gammelFordeling
        assertEquals(0, differanse.fjernedeInntekter.size)
        assertEquals(1, differanse.nyeEllerEndredeInntekter.size)
        assertEquals(
            expected = PeriodeMedBeløp(2 jan 2018 til 3 jan 2018, 1000.0),
            actual = differanse.nyeEllerEndredeInntekter[0]
        )
    }

    @Test
    fun `fjernet to dager`() {
        val perioder = (1 jan 2018 til 3 jan 2018)
        val gammelFordeling = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 3000.0,
        )
        val dato = (1 jan 2018)
        val perioder1 = listOf((dato tilOgMed dato))
        val nyFordeling = TilkommenInntekt.ny(
            fom = perioder1.first().fom,
            tom = perioder1.last().tom,
            dager = perioder1.flatMap { it.datoer() },
            periodebeløp = 1000.0,
        )
        val differanse = nyFordeling differanseFra gammelFordeling
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
        val differanse = nyFordeling differanseFra gammelFordeling
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
        val perioder = (1 jan 2018 til 31 jan 2018)
        val nyFordeling = TilkommenInntekt.ny(
            fom = perioder.first<Periode>().fom,
            tom = perioder.last<Periode>().tom,
            dager = perioder.flatMap<Periode, LocalDate> { it.datoer() },
            periodebeløp = 31000.0,
        )
        val differanse = nyFordeling differanseFra gammelFordeling
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
        val differanse = nyFordeling differanseFra gammelFordeling
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
    }*/

    /*private val ingenFordeling = TilkommenInntekt.ny(
        fom = 1 jan 2018,
        tom = 31 jan 2018,
        dager = emptyList(),
        periodebeløp = 0.0,
    )*/

    private infix fun LocalDate.til(other: Int) = PeriodBuilder(this, other)

    class PeriodBuilder(private val fom: LocalDate, private val int: Int) {
        infix fun jan(target: Int): Periode = fom tilOgMed LocalDate.of(target, Month.JANUARY, int)
    }
}
