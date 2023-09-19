package no.nav.helse.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.modell.saksbehandler.handlinger.SubsumsjonEvent
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubsumsjonsmelderTest {

    private companion object {
        private const val FNR = "12345678910"
    }

    private val testRapid = TestRapid()
    private val subsumsjonsmelder = Subsumsjonsmelder("versjonAvKode", testRapid)

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `bygg kafkamelding`() {
        val id = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()

        val subsumsjonEvent = SubsumsjonEvent(
            id = id,
            fødselsnummer = FNR,
            paragraf = "EN PARAGRAF",
            ledd = "ET LEDD",
            bokstav = "EN BOKSTAV",
            lovverk = "folketrygdloven",
            lovverksversjon = "1970-01-01",
            utfall = "VILKAR_BEREGNET",
            input = mapOf("foo" to "bar"),
            output = mapOf("foo" to "bar"),
            sporing = mapOf("identifikator" to listOf("EN ID")),
            tidsstempel = tidsstempel,
            kilde = "KILDE",
        )

        subsumsjonsmelder.nySubsumsjon(FNR, subsumsjonEvent)

        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("subsumsjon", melding["@event_name"].asText())
        assertEquals(id, melding["id"].asUUID())
        assertEquals(FNR, melding["fodselsnummer"].asText())
        assertEquals("versjonAvKode", melding["versjonAvKode"].asText())
        assertEquals("1.0.0", melding["versjon"].asText())
        assertEquals("EN PARAGRAF", melding["paragraf"].asText())
        assertEquals("ET LEDD", melding["ledd"].asText())
        assertEquals("EN BOKSTAV", melding["bokstav"].asText())
        assertEquals("folketrygdloven", melding["lovverk"].asText())
        assertEquals("1970-01-01", melding["lovverksversjon"].asText())
        assertEquals("VILKAR_BEREGNET", melding["utfall"].asText())
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(melding["input"]))
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(melding["output"]))
        assertEquals(tidsstempel, melding["tidsstempel"].asLocalDateTime())
        assertEquals("KILDE", melding["kilde"].asText())
    }

    @Test
    fun `bygg kafkamelding uten ledd og bokstav`() {
        val id = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()

        val subsumsjonEvent = SubsumsjonEvent(
            id = id,
            fødselsnummer = FNR,
            paragraf = "EN PARAGRAF",
            ledd = null,
            bokstav = null,
            lovverk = "folketrygdloven",
            lovverksversjon = "1970-01-01",
            utfall = "VILKAR_BEREGNET",
            input = mapOf("foo" to "bar"),
            output = mapOf("foo" to "bar"),
            sporing = mapOf("identifikator" to listOf("EN ID")),
            tidsstempel = tidsstempel,
            kilde = "KILDE",
        )

        subsumsjonsmelder.nySubsumsjon(FNR, subsumsjonEvent)

        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("subsumsjon", melding["@event_name"].asText())
        assertEquals(id, melding["id"].asUUID())
        assertEquals(FNR, melding["fodselsnummer"].asText())
        assertEquals("versjonAvKode", melding["versjonAvKode"].asText())
        assertEquals("1.0.0", melding["versjon"].asText())
        assertEquals("EN PARAGRAF", melding["paragraf"].asText())
        assertNull(melding["ledd"])
        assertNull(melding["bokstav"])
        assertEquals("folketrygdloven", melding["lovverk"].asText())
        assertEquals("1970-01-01", melding["lovverksversjon"].asText())
        assertEquals("VILKAR_BEREGNET", melding["utfall"].asText())
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(melding["input"]))
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(melding["output"]))
        assertEquals(tidsstempel, melding["tidsstempel"].asLocalDateTime())
        assertEquals("KILDE", melding["kilde"].asText())
    }
}
