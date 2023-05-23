package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import no.nav.helse.januar
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.løsninger.Saksbehandlerløsning
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class SaksbehandlerløsningTest {

    private companion object {
        private val GODKJENNINGSBEHOV_ID = randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private const val OPPGAVE_ID = 1L
        private const val FNR = "12020052345"
        private const val IDENT = "Z999999"
        private const val HENDELSE_JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
        private const val GODKJENNINGSBEHOV_JSON = """{ "@event_name": "behov", "Godkjenning": {} }"""
        private val objectMapper = jacksonObjectMapper()
    }

    private val hendelseDao = mockk<HendelseDao>(relaxed = true)
    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)

    private fun saksbehandlerløsning(godkjent: Boolean, saksbehandlerløsning: List<UUID> = emptyList()) = Saksbehandlerløsning(
        id = randomUUID(),
        behandlingId = randomUUID(),
        fødselsnummer = FNR,
        json = HENDELSE_JSON,
        godkjent = godkjent,
        saksbehandlerIdent = IDENT,
        epostadresse = "saksbehandler@nav.no",
        godkjenttidspunkt = GODKJENTTIDSPUNKT,
        årsak = null,
        begrunnelser = null,
        kommentar = null,
        saksbehandleroverstyringer = saksbehandlerløsning,
        oppgaveId = OPPGAVE_ID,
        godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
        hendelseDao = hendelseDao,
        oppgaveDao = mockk(relaxed = true),
        godkjenningMediator = GodkjenningMediator(
            mockk(relaxed = true),
            mockk()
        ),
        utbetalingDao = utbetalingDao,
        sykefraværstilfelle = Sykefraværstilfelle(FNR, 1.januar, listOf(Generasjon(randomUUID(), randomUUID(), 1.januar, 31.januar, 1.januar))),
    )

    private val context = CommandContext(randomUUID())

    @Test
    fun `løser godkjenningsbehov`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns GODKJENNINGSBEHOV_JSON
        every { utbetalingDao.utbetalingFor(OPPGAVE_ID) } returns Utbetaling(
            randomUUID(),
            1000,
            1000,
            Utbetalingtype.UTBETALING,
        )
        val saksbehandlerløsning = saksbehandlerløsning(true)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(true, DELVIS_REFUSJON)
    }

    @Test
    fun `løser godkjenningsbehov med saksbehandleroverstyringer`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns GODKJENNINGSBEHOV_JSON
        every { utbetalingDao.utbetalingFor(OPPGAVE_ID) } returns Utbetaling(
            randomUUID(),
            1000,
            1000,
            Utbetalingtype.UTBETALING,
        )
        val  saksbehandleroverstyringer = listOf(randomUUID(), randomUUID())
        val saksbehandlerløsning = saksbehandlerløsning(true, saksbehandleroverstyringer)
        assertTrue(saksbehandlerløsning.execute(context))
        val løsning = context.meldinger()
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") } ?: fail("Fant ikke løsning på godkjenningsbehov")

        val godkjenning = løsning.path("@løsning").path("Godkjenning")
        assertEquals(objectMapper.valueToTree(saksbehandleroverstyringer), godkjenning.path("saksbehandleroverstyringer"))
    }

    @Test
    fun `løser godkjenningsbehov ved avvist utbetaling`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns GODKJENNINGSBEHOV_JSON
        every { utbetalingDao.utbetalingFor(OPPGAVE_ID) } returns Utbetaling(
            randomUUID(),
            1000,
            1000,
            Utbetalingtype.UTBETALING,
        )
        val saksbehandlerløsning = saksbehandlerløsning(false)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(false, DELVIS_REFUSJON)
    }

    private fun assertLøsning(godkjent: Boolean, refusjonstype: Refusjonstype) {
        val løsning = context.meldinger()
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") } ?: fail("Fant ikke løsning på godkjenningsbehov")

        assertJsonEquals(GODKJENNINGSBEHOV_JSON, løsning)

        val godkjenning = løsning.path("@løsning").path("Godkjenning")
        assertTrue(godkjenning.path("godkjent").isBoolean)
        assertEquals(godkjent, godkjenning.path("godkjent").booleanValue())
        assertEquals(IDENT, godkjenning.path("saksbehandlerIdent").textValue())
        assertEquals(GODKJENTTIDSPUNKT, LocalDateTime.parse(godkjenning.path("godkjenttidspunkt").textValue()))
        assertEquals(refusjonstype, enumValueOf<Refusjonstype>(godkjenning.path("refusjontype").asText()))
        assertTrue(godkjenning.path("årsak").isNull)
        assertTrue(godkjenning.path("kommentar").isNull)
        assertTrue(godkjenning.path("begrunnelser").isNull)
        assertTrue(godkjenning.path("saksbehandleroverstyringer").isEmpty)
    }

    private fun assertJsonEquals(expected: String, actual: JsonNode) {
        val expectedJson = objectMapper.readTree(expected)
        assertJsonEquals(expectedJson, actual)
    }

    private fun assertJsonEquals(field: String, expected: JsonNode, actual: JsonNode) {
        assertEquals(
            expected.nodeType,
            actual.nodeType
        ) { "Field <$field> was not of expected value. Expected <${expected.nodeType}> got <${actual.nodeType}>" }
        when (expected.nodeType) {
            JsonNodeType.OBJECT -> assertJsonEquals(expected, actual)
            else -> assertEquals(
                expected,
                actual
            ) { "Field <$field> was not of expected value. Expected <${expected}> got <${actual}>" }
        }
    }

    private fun assertJsonEquals(expected: JsonNode, actual: JsonNode) {
        expected.fieldNames().forEach { field ->
            assertTrue(actual.has(field)) { "Expected field <$field> to exist" }
            assertJsonEquals(field, expected.path(field), actual.path(field))
        }
    }

}
