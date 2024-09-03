package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.UtbetalingsgodkjenningCommand
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

internal class SaksbehandlerløsningTest {

    private companion object {
        private val GODKJENNINGSBEHOV_ID = randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private const val FNR = "12020052345"
        private const val IDENT = "Z999999"
        private const val GODKJENNINGSBEHOV_JSON = """{ "@event_name": "behov", "Godkjenning": {} }"""
        private val objectMapper = jacksonObjectMapper()
    }

    private val meldingDao = mockk<MeldingDao>(relaxed = true)

    private val saksbehandler = Saksbehandlerløsning.Saksbehandler(
        ident = "saksbehandlerident",
        epostadresse = "saksbehandler@nav.no"
    )

    private val beslutter = Saksbehandlerløsning.Saksbehandler(
        ident = "beslutterident",
        epostadresse = "beslutter@nav.no"
    )

    private fun saksbehandlerløsning(godkjent: Boolean, saksbehandlerløsning: List<UUID> = emptyList(), arbeidsgiverbeløp: Int = 0, personbeløp: Int = 0): UtbetalingsgodkjenningCommand {
        val vedtaksperiodeId = randomUUID()
        return UtbetalingsgodkjenningCommand(
            fødselsnummer = FNR,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = null,
            utbetaling = Utbetaling(randomUUID(), arbeidsgiverbeløp, personbeløp, Utbetalingtype.UTBETALING),
            sykefraværstilfelle = Sykefraværstilfelle(
                fødselsnummer = FNR,
                skjæringstidspunkt = 1.januar,
                gjeldendeGenerasjoner = listOf(Generasjon(randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar))
            ),
            godkjent = godkjent,
            godkjenttidspunkt = GODKJENTTIDSPUNKT,
            ident = IDENT,
            epostadresse = "saksbehandler@nav.no",
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = saksbehandlerløsning,
            godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            meldingDao = meldingDao,
            godkjenningMediator = GodkjenningMediator(mockk(relaxed = true), mockk(), mockk(), mockk(), mockk(), mockk(relaxed = true))
        )
    }

    private val observer = object : CommandContextObserver {
        val hendelser = mutableListOf<String>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {}

        override fun hendelse(hendelse: String) {
            this.hendelser.add(hendelse)
        }
    }

    private val context = CommandContext(randomUUID()).also {
        it.nyObserver(observer)
    }

    @Test
    fun `løser godkjenningsbehov`() {
        every { meldingDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns GODKJENNINGSBEHOV_JSON
        val saksbehandlerløsning = saksbehandlerløsning(true, arbeidsgiverbeløp = 1000, personbeløp = 1000)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(true, DELVIS_REFUSJON)
    }

    @Test
    fun `løser godkjenningsbehov med saksbehandleroverstyringer`() {
        every { meldingDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns GODKJENNINGSBEHOV_JSON
        val  saksbehandleroverstyringer = listOf(randomUUID(), randomUUID())
        val saksbehandlerløsning = saksbehandlerløsning(true, saksbehandleroverstyringer)
        assertTrue(saksbehandlerløsning.execute(context))
        val løsning = observer.hendelser
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") } ?: fail("Fant ikke løsning på godkjenningsbehov")

        val godkjenning = løsning.path("@løsning").path("Godkjenning")
        assertEquals(objectMapper.valueToTree(saksbehandleroverstyringer), godkjenning.path("saksbehandleroverstyringer"))
    }

    @Test
    fun `løser godkjenningsbehov ved avvist utbetaling`() {
        every { meldingDao.finnUtbetalingsgodkjenningbehovJson(GODKJENNINGSBEHOV_ID) } returns GODKJENNINGSBEHOV_JSON
        val saksbehandlerløsning = saksbehandlerløsning(false, arbeidsgiverbeløp = 1000, personbeløp = 1000)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(false, DELVIS_REFUSJON)
    }

    private fun assertLøsning(godkjent: Boolean, refusjonstype: Refusjonstype) {
        val løsning = observer.hendelser
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
