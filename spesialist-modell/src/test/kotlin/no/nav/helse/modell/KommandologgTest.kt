package no.nav.helse.modell

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KommandologgTest {

    @Test
    fun `Kan lage nytt innslag`() {
        val logg = Kommandologg()
        logg.nyttInnslag("En melding")
        val innslag = logg.alleInnslag()
        assertEquals(1, innslag.size)
        assertEquals("En melding", innslag.single().melding)
    }

    @Test
    fun `Kan legge til kontekst`() {
        val logg = Kommandologg()
        logg.kontekst("En kontekst")
        logg.nyttInnslag("En melding")
        val innslaget = logg.alleInnslag().single()
        assertEquals("En melding", innslaget.melding)
        assertEquals(1, innslaget.kontekster.size)
        assertEquals("En kontekst", innslaget.kontekster.single().navn)
    }

    @Test
    fun `Kan lage flere innslag`() {
        val logg = Kommandologg()
        logg.nyttInnslag("En melding")
        logg.nyttInnslag("En annen melding")
        assertEquals(2, logg.alleInnslag().size)
    }

    @Test
    fun `Kan legge til forskjellige kontekster`() {
        val logg = Kommandologg()
        logg.kontekst("En kontekst")
        logg.kontekst("En annen kontekst")
        logg.nyttInnslag("En melding")
        assertEquals(2, logg.alleInnslag().single().kontekster.size)
    }

    @Test
    fun `Legger ikke til samme kontekst flere ganger`() {
        val logg = Kommandologg()
        logg.kontekst("En kontekst")
        logg.kontekst("En kontekst")
        logg.nyttInnslag("En melding")
        assertEquals(1, logg.alleInnslag().single().kontekster.size)
    }
}