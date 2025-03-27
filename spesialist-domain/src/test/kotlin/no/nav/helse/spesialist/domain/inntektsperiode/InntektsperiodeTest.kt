package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
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
        val endring = Inntektsperiodeendring.ny(
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            fordeling = Inntektsfordeling(
                dager = listOf(1 jan 2018, 2 jan 2018, 15 jan 2018, 31 jan 2018),
                periodebeløp = 40000.0,
            )
        )
        val inntektsendringer = inntektsperiode.endring(endring)
        assertEquals(organisasjonsnummer, inntektsendringer.organisasjonsnummer)
        assertEquals(1, inntektsperiode.endringer.size)
        assertEquals(endring, inntektsperiode.endringer[0])
        assertEquals(3, inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(0, inntektsendringer.fjernedeInntekter.size)
    }

    @Test
    fun `Når det finnes en tidligere fordeling representerer inntektsendringer kun differansen mellom ny og gammel`() {
        val inntektsperiode = Inntektsperiode.ny(
            fødselsnummer = lagFødselsnummer(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
        )
        val endring = Inntektsperiodeendring.ny(
            fom = 1 jan 2018,
            tom = 3 jan 2018,
            fordeling = Inntektsfordeling(
                dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018),
                periodebeløp = 30000.0,
            )
        )
        inntektsperiode.endring(endring)
        val nyEndring = Inntektsperiodeendring.ny(
            fom = 1 jan 2018,
            tom = 3 jan 2018,
            fordeling = Inntektsfordeling(
                dager = listOf(1 jan 2018, 2 jan 2018),
                periodebeløp = 20000.0,
            )
        )
        val inntektsendringer = inntektsperiode.endring(nyEndring)
        assertEquals(2, inntektsperiode.endringer.size)
        assertEquals(0, inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(1, inntektsendringer.fjernedeInntekter.size)
        assertEquals(Periode(3 jan 2018, 3 jan 2018), inntektsendringer.fjernedeInntekter[0])
    }

    @Test
    fun `Når det finnes flere tidligere fordelinger legges den siste av disse til grunn ved utregning av differanse`() {
        val inntektsperiode = Inntektsperiode.ny(
            fødselsnummer = lagFødselsnummer(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
        )
        val endring1 = Inntektsperiodeendring.ny(
            fom = 1 jan 2018,
            tom = 4 jan 2018,
            fordeling = Inntektsfordeling(
                dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018, 4 jan 2018),
                periodebeløp = 40000.0,
            )
        )
        inntektsperiode.endring(endring1)
        val endring2 = Inntektsperiodeendring.ny(
            fom = 1 jan 2018,
            tom = 4 jan 2018,
            fordeling = Inntektsfordeling(
                dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018),
                periodebeløp = 30000.0,
            )
        )
        inntektsperiode.endring(endring2)
        val endring3 = Inntektsperiodeendring.ny(
            fom = 1 jan 2018,
            tom = 4 jan 2018,
            fordeling = Inntektsfordeling(
                dager = listOf(1 jan 2018, 2 jan 2018),
                periodebeløp = 20000.0,
            )
        )
        val inntektsendringer = inntektsperiode.endring(endring3)
        assertEquals(3, inntektsperiode.endringer.size)
        assertEquals(0, inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(1, inntektsendringer.fjernedeInntekter.size)
        assertEquals(Periode(3 jan 2018, 3 jan 2018), inntektsendringer.fjernedeInntekter[0])
    }
}
