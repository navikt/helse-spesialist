package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KommandologgVisitorTest {

    @Test
    fun `Kan besøke et helt tre av kommandologger`() {
         val rot = Kommandologg.nyLogg()
        rot.kontekst("Rotkontekst")
        rot.nyttInnslag("Innslag fra rot")
        rot.barn().also { barn ->
            barn.kontekst("Barnkontekst")
            barn.nyttInnslag("Innslag fra barn")
            barn.barn().also { barnebarn ->
                barnebarn.kontekst("Barnebarnkontekst")
                barnebarn.nyttInnslag("Innslag fra barnebarn")
            }
        }
        rot.barn().also { søskenAvBarn ->
            søskenAvBarn.kontekst("Søskenkontekst")
            søskenAvBarn.nyttInnslag("Innslag fra søsken")
        }
        rot.accept(visitor)
        assertEquals(4, visitor.innslag.size)
    }

    private val visitor = object : KommandologgVisitor {
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