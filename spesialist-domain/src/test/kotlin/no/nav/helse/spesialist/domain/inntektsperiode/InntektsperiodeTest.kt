package no.nav.helse.spesialist.domain.inntektsperiode

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
            fom = 1 jan 2018,
            tom = 31 jan 2018,
        )
        val fordeling = Inntektsfordeling.ny(
            dager = listOf(
                Inntektsdag(dato = 1 jan 2018, beløp = 10000.0),
                Inntektsdag(dato = 2 jan 2018, beløp = 10000.0),
                Inntektsdag(dato = 15 jan 2018, beløp = 10000.0),
                Inntektsdag(dato = 31 jan 2018, beløp = 10000.0),
            )
        )
        val inntektsendringer = inntektsperiode.nyFordeling(fordeling)
        assertEquals(organisasjonsnummer, inntektsendringer.organisasjonsnummer)
        assertEquals(Inntektsendringer.PeriodeMedBeløp(1 jan 2018, 2 jan 2018, 10000.0), inntektsendringer.nyeEllerEndredeInntekter[0])
        assertEquals(Inntektsendringer.PeriodeMedBeløp(15 jan 2018, 15 jan 2018, 10000.0), inntektsendringer.nyeEllerEndredeInntekter[1])
        assertEquals(Inntektsendringer.PeriodeMedBeløp(31 jan 2018, 31 jan 2018, 10000.0), inntektsendringer.nyeEllerEndredeInntekter[2])
        assertEquals(3, inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(1, inntektsperiode.fordelinger.size)
        assertEquals(fordeling, inntektsperiode.fordelinger[0])
        assertEquals(0, inntektsendringer.fjernedeInntekter.size)
    }
}
