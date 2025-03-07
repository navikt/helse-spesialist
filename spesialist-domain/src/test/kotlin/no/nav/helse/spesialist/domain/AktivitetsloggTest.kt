package no.nav.helse.spesialist.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AktivitetsloggTest {
    @Test
    fun `legg til info`() {
        val aktivitetslogg = Aktivitetslogg("Foo")
        aktivitetslogg.info("Bar", "foo" to "bar")

        val aktivitet = aktivitetslogg.meldinger().single()
        assertEquals(Aktivitet("Bar", kontekst = mapOf("foo" to "bar")), aktivitet)
        assertEquals(listOf("Foo"), aktivitet.lokasjon())
    }

    @Test
    fun `propager melding til forelder`() {
        val forelder = Aktivitetslogg("forelder")
        val barn = Aktivitetslogg("barn", forelder = forelder)
        barn.info("Bar", "foo" to "bar")

        val aktivitet = forelder.meldinger().single()
        assertEquals(Aktivitet("Bar", kontekst = mapOf("foo" to "bar")), aktivitet)
        assertEquals(listOf("forelder", "barn"), aktivitet.lokasjon())
    }

    @Test
    fun `kan legge til flere meldinger`() {
        val forelder = Aktivitetslogg("forelder")
        val barn = Aktivitetslogg("barn", forelder = forelder)
        barn.info("Foo", "foo" to "bar")
        barn.info("Bar", "foo" to "bar")

        val aktivitet1 = forelder.meldinger()[0]
        val aktivitet2 = forelder.meldinger()[1]
        assertEquals(Aktivitet("Foo", kontekst = mapOf("foo" to "bar")), aktivitet1)
        assertEquals(Aktivitet("Bar", kontekst = mapOf("foo" to "bar")), aktivitet2)
        assertEquals(listOf("forelder", "barn"), aktivitet1.lokasjon())
        assertEquals(listOf("forelder", "barn"), aktivitet2.lokasjon())
    }

    @Test
    fun `kan erstatte forelder`() {
        val forelder1 = Aktivitetslogg("forelder1")
        val forelder2 = Aktivitetslogg("forelder2")
        val barn = Aktivitetslogg("barn", forelder = forelder1)
        barn.info("Foo", "foo" to "bar")
        barn.nyForelder(forelder2)
        barn.info("Bar", "foo" to "bar")

        val aktivitet1 = forelder1.meldinger().single()
        val aktivitet2 = forelder2.meldinger().single()
        assertEquals(Aktivitet("Foo", kontekst = mapOf("foo" to "bar")), aktivitet1)
        assertEquals(Aktivitet("Bar", kontekst = mapOf("foo" to "bar")), aktivitet2)
        assertEquals(listOf("forelder1", "barn"), aktivitet1.lokasjon())
        assertEquals(listOf("forelder2", "barn"), aktivitet2.lokasjon())
    }
}
