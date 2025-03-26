package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.inntektsperiode.Inntektsendringer.PeriodeMedBeløp
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import kotlin.test.Test
import kotlin.test.assertEquals

class InntektsperiodeTest {

    @Test
    fun `kan lage ny inntektsperiode`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val inntektsperiode = Inntektsperiode.ny(
            fødselsnummer = lagFødselsnummer(),
            organisasjonsnummer = organisasjonsnummer,
        )
        val fordeling = Inntektsfordeling.ny(
            periodebeløp = 40000.0,
            dager = listOf(1 jan 2018, 2 jan 2018, 15 jan 2018, 31 jan 2018)
        )
        val inntektsendringer = inntektsperiode.nyFordeling(fordeling)
        assertEquals(organisasjonsnummer, inntektsendringer.organisasjonsnummer)
        assertEquals(
            expected = PeriodeMedBeløp(Periode(1 jan 2018, 2 jan 2018), 10000.0),
            actual = inntektsendringer.nyeEllerEndredeInntekter[0]
        )
        assertEquals(
            expected = PeriodeMedBeløp(Periode(15 jan 2018, 15 jan 2018), 10000.0),
            actual = inntektsendringer.nyeEllerEndredeInntekter[1]
        )
        assertEquals(
            expected = PeriodeMedBeløp(Periode(31 jan 2018, 31 jan 2018), 10000.0),
            actual = inntektsendringer.nyeEllerEndredeInntekter[2]
        )
        assertEquals(3, inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(1, inntektsperiode.fordelinger.size)
        assertEquals(fordeling, inntektsperiode.fordelinger[0])
        assertEquals(0, inntektsendringer.fjernedeInntekter.size)
    }
}
