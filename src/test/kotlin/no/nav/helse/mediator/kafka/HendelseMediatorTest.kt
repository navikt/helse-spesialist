package no.nav.helse.mediator.kafka

import io.mockk.mockk
import no.nav.helse.api.TilbakerullingDTO
import no.nav.helse.api.TilbakerullingMedSlettingDTO
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelseMediatorTest {

    private val testRapid = TestRapid()

    private val mediator = HendelseMediator(
        testRapid,
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        MiljøstyrtFeatureToggle(emptyMap())
    )

    @Test
    fun `publiserer tilbakerulling på rapid`() {
        mediator.håndter(TilbakerullingDTO("FNR", "AKTØRID", 1L))
        assertEquals("rollback_person", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals("FNR", testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals("AKTØRID", testRapid.inspektør.field(0, "aktørId").asText())
        assertEquals(1L, testRapid.inspektør.field(0, "personVersjon").asLong())
    }

    @Test
    fun `publiserer tilbakerulling med sletting på rapid`() {
        mediator.håndter(TilbakerullingMedSlettingDTO("FNR", "AKTØRID"))
        assertEquals("rollback_person_delete", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals("FNR", testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals("AKTØRID", testRapid.inspektør.field(0, "aktørId").asText())
    }
}
