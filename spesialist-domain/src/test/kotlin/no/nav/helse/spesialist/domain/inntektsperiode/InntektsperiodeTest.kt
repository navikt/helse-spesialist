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
        val fordeling = Inntektsfordeling.ny(
            periodebeløp = 40000.0,
            dager = listOf(1 jan 2018, 2 jan 2018, 15 jan 2018, 31 jan 2018)
        )
        val inntektsendringer = inntektsperiode.nyFordeling(fordeling)
        assertEquals(organisasjonsnummer, inntektsendringer.organisasjonsnummer)
        assertEquals(1, inntektsperiode.fordelinger.size)
        assertEquals(fordeling, inntektsperiode.fordelinger[0])
        assertEquals(3, inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(0, inntektsendringer.fjernedeInntekter.size)
    }

    @Test
    fun `Når det finnes en tidligere fordeling representerer inntektsendringer kun differansen mellom ny og gammel`() {
        val inntektsperiode = Inntektsperiode.ny(
            fødselsnummer = lagFødselsnummer(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
        )
        val fordeling = Inntektsfordeling.ny(
            periodebeløp = 30000.0,
            dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018)
        )
        inntektsperiode.nyFordeling(fordeling)
        val nyFordeling = Inntektsfordeling.ny(
            periodebeløp = 20000.0,
            dager = listOf(1 jan 2018, 2 jan 2018)
        )
        val inntektsendringer = inntektsperiode.nyFordeling(nyFordeling)
        assertEquals(2, inntektsperiode.fordelinger.size)
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
        val fordeling1 = Inntektsfordeling.ny(
            periodebeløp = 40000.0,
            dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018, 4 jan 2018)
        )
        inntektsperiode.nyFordeling(fordeling1)
        val fordeling2 = Inntektsfordeling.ny(
            periodebeløp = 30000.0,
            dager = listOf(1 jan 2018, 2 jan 2018, 3 jan 2018)
        )
        inntektsperiode.nyFordeling(fordeling2)
        val fordeling3 = Inntektsfordeling.ny(
            periodebeløp = 20000.0,
            dager = listOf(1 jan 2018, 2 jan 2018)
        )
        val inntektsendringer = inntektsperiode.nyFordeling(fordeling3)
        assertEquals(3, inntektsperiode.fordelinger.size)
        assertEquals(expected = 0, actual = inntektsendringer.nyeEllerEndredeInntekter.size)
        assertEquals(1, inntektsendringer.fjernedeInntekter.size)
        assertEquals(Periode(3 jan 2018, 3 jan 2018), inntektsendringer.fjernedeInntekter[0])
    }
}
