package no.nav.helse.spesialist.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AktivitetsloggTest {
    @Test
    fun `legg til info`() {
        val aktivitetslogg = Aktivitetslogg("lokasjon")
        aktivitetslogg.info("tekst", "key" to "value")

        val aktiviteter = aktivitetslogg.aktiviteter()
        assertEquals(1, aktiviteter.size)
        assertEquals(mapOf(listOf("lokasjon") to listOf(Aktivitet("tekst", kontekst = mapOf("key" to "value")))), aktiviteter)
    }

    @Test
    fun `propager melding til forelder`() {
        val forelder = Aktivitetslogg("forelder")
        val barn = Aktivitetslogg("barn", forelder = forelder)
        barn.info("tekst", "key" to "value")

        val aktiviteter = forelder.aktiviteter()
        assertEquals(1, aktiviteter.size)
        assertEquals(mapOf(listOf("forelder", "barn") to listOf(Aktivitet("tekst", kontekst = mapOf("key" to "value")))), aktiviteter)
    }

    @Test
    fun `kan legge til flere aktiviteter`() {
        val forelder = Aktivitetslogg("forelder")
        val barn = Aktivitetslogg("barn", forelder = forelder)
        barn.info("tekst1", "key1" to "value1")
        barn.info("tekst2", "key2" to "value2")

        val aktiviteter = forelder.aktiviteter()
        assertEquals(
            mapOf(
                listOf("forelder", "barn") to listOf(
                    Aktivitet(
                        "tekst1",
                        kontekst = mapOf("key1" to "value1")
                    ),
                    Aktivitet(
                        "tekst2",
                        kontekst = mapOf("key2" to "value2")
                    ),
                )
            ),
            aktiviteter
        )
    }

    @Test
    fun `kan erstatte forelder`() {
        val forelder1 = Aktivitetslogg("forelder1")
        val forelder2 = Aktivitetslogg("forelder2")
        val barn = Aktivitetslogg("barn", forelder = forelder1)
        barn.info("tekst1", "key1" to "value1")
        barn.nyForelder(forelder2)
        barn.info("tekst2", "key2" to "value2")

        val aktiviteter1 = forelder1.aktiviteter()
        val aktiviteter2 = forelder2.aktiviteter()
        assertEquals(
            mapOf(
                listOf("forelder1", "barn") to listOf(
                    Aktivitet(
                        "tekst1",
                        kontekst = mapOf("key1" to "value1")
                    ),
                )
            ),
            aktiviteter1
        )
        assertEquals(
            mapOf(
                listOf("forelder2", "barn") to listOf(
                    Aktivitet(
                        "tekst2",
                        kontekst = mapOf("key2" to "value2")
                    ),
                )
            ),
            aktiviteter2
        )
    }
}
