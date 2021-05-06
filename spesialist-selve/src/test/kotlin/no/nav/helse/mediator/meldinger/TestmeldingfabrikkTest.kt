package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class TestmeldingfabrikkTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val FNR = "${UUID.randomUUID()}"
        private val AKTØR = "${UUID.randomUUID()}"
        private val HENDELSE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
    }

    private val fabrikk = Testmeldingfabrikk(FNR, AKTØR)

    @Test
    fun `meldinger inneholder standardfelt`() {
        assertStandardfelt(fabrikk.lagVedtaksperiodeEndret())
        assertStandardfelt(fabrikk.lagVedtaksperiodeForkastet())
        assertStandardfelt(fabrikk.lagGodkjenningsbehov())
    }

    @Test
    fun `vedtaksperiode endret`() {
        val melding = fabrikk.lagVedtaksperiodeEndret(HENDELSE_ID, VEDTAKSPERIODE_ID, "orgnr", "START", "SLUTT")
        assertFelt("fødselsnummer", FNR, melding)
        assertFelt("aktørId", AKTØR, melding)
        assertFelt("organisasjonsnummer", "orgnr", melding)
        assertFelt("@id", "$HENDELSE_ID", melding)
        assertFelt("vedtaksperiodeId", "$VEDTAKSPERIODE_ID", melding)
        assertFelt("forrigeTilstand", "START", melding)
        assertFelt("gjeldendeTilstand", "SLUTT", melding)
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        val melding = fabrikk.lagVedtaksperiodeForkastet(HENDELSE_ID, VEDTAKSPERIODE_ID, "orgnr")
        assertFelt("fødselsnummer", FNR, melding)
        assertFelt("aktørId", AKTØR, melding)
        assertFelt("organisasjonsnummer", "orgnr", melding)
        assertFelt("@id", "$HENDELSE_ID", melding)
        assertFelt("vedtaksperiodeId", "$VEDTAKSPERIODE_ID", melding)
    }

    @Test
    fun godkjenningsbehov() {
        val melding = fabrikk.lagGodkjenningsbehov(HENDELSE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID,"orgnr")
        assertFelt("fødselsnummer", FNR, melding)
        assertFelt("aktørId", AKTØR, melding)
        assertFelt("organisasjonsnummer", "orgnr", melding)
        assertFelt("@id", "$HENDELSE_ID", melding)
        assertFelt("vedtaksperiodeId", "$VEDTAKSPERIODE_ID", melding)
    }

    private fun assertStandardfelt(melding: String) {
        objectMapper.readTree(melding).also { json ->
            assertTrue(json.hasNonNull("@id"))
            assertTrue(json.hasNonNull("@opprettet"))
            assertTrue(json.hasNonNull("@event_name"))
        }
    }

    private fun assertFelt(felt: String, forventet: String, melding: String) {
        objectMapper.readTree(melding).also { json ->
            assertEquals(forventet, json.path(felt).asText())
        }
    }
}
