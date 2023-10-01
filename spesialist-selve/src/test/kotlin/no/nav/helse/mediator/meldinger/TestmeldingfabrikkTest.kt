package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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

    @Test
    fun `meldinger inneholder standardfelt`() {
        assertStandardfelt(Testmeldingfabrikk.lagVedtaksperiodeEndret(aktørId = AKTØR, fødselsnummer = FNR))
        assertStandardfelt(Testmeldingfabrikk.lagVedtaksperiodeForkastet(FNR, AKTØR))
        assertStandardfelt(Testmeldingfabrikk.lagGodkjenningsbehov(AKTØR, FNR))
    }

    @Test
    fun `vedtaksperiode endret`() {
        val melding = Testmeldingfabrikk.lagVedtaksperiodeEndret(HENDELSE_ID, AKTØR, FNR, "orgnr", VEDTAKSPERIODE_ID, "START", "SLUTT")
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
        val melding = Testmeldingfabrikk.lagVedtaksperiodeForkastet(AKTØR, FNR, VEDTAKSPERIODE_ID, "orgnr", HENDELSE_ID)
        assertFelt("fødselsnummer", FNR, melding)
        assertFelt("aktørId", AKTØR, melding)
        assertFelt("organisasjonsnummer", "orgnr", melding)
        assertFelt("@id", "$HENDELSE_ID", melding)
        assertFelt("vedtaksperiodeId", "$VEDTAKSPERIODE_ID", melding)
    }

    @Test
    fun godkjenningsbehov() {
        val melding =
            Testmeldingfabrikk.lagGodkjenningsbehov(AKTØR, FNR, VEDTAKSPERIODE_ID, UTBETALING_ID, id = HENDELSE_ID)
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
