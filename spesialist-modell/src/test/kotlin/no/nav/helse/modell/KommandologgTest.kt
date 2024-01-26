package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KommandologgTest {

    @Test
    fun `Kan lage nytt innslag`() {
        val logg = Kommandologg.nyLogg()
        logg.nyttInnslag("En melding")
        val innslag = logg.alleInnslag()
        assertEquals(1, innslag.size)
        assertEquals("En melding", innslag.single().melding)
    }

    @Test
    fun `Kan legge til kontekst`() {
        val logg = Kommandologg.nyLogg()
        logg.kontekst("En kontekst")
        logg.nyttInnslag("En melding")
        val innslaget = logg.alleInnslag().single()
        assertEquals("En melding", innslaget.melding)
        assertEquals(1, innslaget.kontekster.size)
        assertEquals("En kontekst", innslaget.kontekster.single())
    }

    @Test
    fun `Kan lage flere innslag`() {
        val logg = Kommandologg.nyLogg()
        logg.nyttInnslag("En melding")
        logg.nyttInnslag("En annen melding")
        assertEquals(2, logg.alleInnslag().size)
    }

    @Test
    fun `Kan legge til forskjellige kontekster`() {
        val logg = Kommandologg.nyLogg()
        logg.kontekst("En kontekst")
        logg.kontekst("En annen kontekst")
        logg.nyttInnslag("En melding")
        assertEquals(2, logg.alleInnslag().single().kontekster.size)
    }

    @Test
    fun `Legger ikke til samme kontekst flere ganger`() {
        val logg = Kommandologg.nyLogg()
        logg.kontekst("En kontekst")
        logg.kontekst("En kontekst")
        logg.nyttInnslag("En melding")
        assertEquals(1, logg.alleInnslag().single().kontekster.size)
    }

    @Test
    fun `Logginnslag blir videresendt til forelder`() {
        val rotlogg = Kommandologg.nyLogg()
        val barn = rotlogg.barn()

        barn.kontekst("En kontekst")
        barn.nyttInnslag("En melding")
        val innslaget = rotlogg.alleInnslag().single()
        assertEquals("En melding", innslaget.melding)
        assertEquals(1, innslaget.kontekster.size)
        assertEquals("En kontekst", innslaget.kontekster.single())
    }

    @Test
    fun `Barn arver kontekster fra forelder`() {
        val forelder = Kommandologg.nyLogg()
        forelder.kontekst("En forelder-kontekst")

        val barn = forelder.barn()

        barn.nyttInnslag("En melding")

        val barninnslaget = forelder.alleInnslag().single()

        assertEquals("En melding", barninnslaget.melding)
        assertEquals(1, barninnslaget.kontekster.size)
        assertEquals("En forelder-kontekst", barninnslaget.kontekster.single())
    }

    @Test
    fun `Barn arver ikke kontekster fra forelder som dukker opp etter at barn har blitt opprettet`() {
        val forelder = Kommandologg.nyLogg()
        val barn = forelder.barn()

        forelder.kontekst("En forelder-kontekst")
        barn.kontekst("En barn-kontekst")
        barn.nyttInnslag("En melding")

        val forelderinnslaget = forelder.alleInnslag().single()
        val barninnslaget = barn.alleInnslag().single()

        assertEquals("En melding", forelderinnslaget.melding)
        assertEquals(1, forelderinnslaget.kontekster.size)
        assertEquals("En barn-kontekst", forelderinnslaget.kontekster.single())

        assertEquals("En melding", barninnslaget.melding)
        assertEquals(1, barninnslaget.kontekster.size)
        assertEquals("En barn-kontekst", barninnslaget.kontekster.single())
    }

    @Test
    fun `Barnebarns innslag propageres opp til rot`() {
        val forelder = Kommandologg.nyLogg()

        forelder.kontekst("En forelder-kontekst")
        forelder.nyttInnslag("En melding fra rot")

        val barn = forelder.barn()

        barn.kontekst("En barn-kontekst")
        barn.nyttInnslag("En melding fra barn")

        val barnebarn = barn.barn()

        barnebarn.kontekst("En barnebarn-kontekst")
        barnebarn.nyttInnslag("En melding fra barnebarn")

        val alleInnslag = forelder.alleInnslag()
        val forelderinnslaget = alleInnslag[0]
        val barninnslaget = alleInnslag[1]
        val barnebarninnslaget = alleInnslag[2]

        assertEquals(3, alleInnslag.size)
        assertEquals(1, forelderinnslaget.kontekster.size)
        assertEquals(2, barninnslaget.kontekster.size)
        assertEquals(3, barnebarninnslaget.kontekster.size)
    }

    private fun Kommandologg.alleInnslag(): List<TestInnslag> {
        val visitor = visitor()
        this.accept(visitor)
        return visitor.innslag.toList()
    }

    private fun visitor() = object : KommandologgVisitor {
        val innslag = mutableListOf<TestInnslag>()
        override fun visitInnslag(id: UUID, opprettet: LocalDateTime, melding: String, kontekster: List<String>) {
            innslag.add(TestInnslag(id, opprettet, melding, kontekster))
        }
    }

    private data class TestInnslag(
        val id: UUID,
        val opprettet: LocalDateTime,
        val melding: String,
        val kontekster: List<String>
    )
}