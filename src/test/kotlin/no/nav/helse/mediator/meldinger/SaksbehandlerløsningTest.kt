package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertNotNull

internal class SaksbehandlerløsningTest {

    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val OID = UUID.randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private const val OPPGAVE_ID = 1L
        private const val FNR = "12020052345"
        private const val IDENT = "Z999999"
        private const val HENDELSE_JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
        private const val GODKJENNINGSBEHOV_JSON = """{ "@event_name": "behov" }"""
        private val objectMapper = jacksonObjectMapper()
    }

    private val hendelseDao = mockk<HendelseDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val arbeidsgiverDao = mockk<ArbeidsgiverDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        commandContextDao = commandContextDao,
        snapshotDao = snapshotDao,
        oppgaveDao = oppgaveDao,
        risikovurderingDao = risikovurderingDao,
        speilSnapshotRestClient = restClient,
        oppgaveMediator = oppgaveMediator,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = mockk(),
        overstyringDao = mockk(),
        digitalKontaktinformasjonDao = mockk(relaxed = true),
        åpneGosysOppgaverDao = mockk(relaxed = true),
        egenAnsattDao = mockk(),
        miljøstyrtFeatureToggle = mockk(relaxed = true),
        automatisering = mockk(relaxed = true),
        utbetalingDao = mockk(relaxed = true),
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao)
    )

    private val godkjenningsbehov = UtbetalingsgodkjenningMessage(GODKJENNINGSBEHOV_JSON)
    private fun saksbehandlerløsning(godkjent: Boolean) = hendelsefabrikk.saksbehandlerløsning(
        id = HENDELSE_ID,
        godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
        fødselsnummer = FNR,
        godkjent = godkjent,
        saksbehandlerident = IDENT,
        oid = OID,
        epostadresse = "saksbehandler@nav.no",
        godkjenttidspunkt = GODKJENTTIDSPUNKT,
        årsak = null,
        begrunnelser = null,
        kommentar = null,
        oppgaveId = OPPGAVE_ID,
        json = HENDELSE_JSON
    )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `løser godkjenningsbehov`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns godkjenningsbehov
        val saksbehandlerløsning = saksbehandlerløsning(true)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(true)
        verify(exactly = 1) { oppgaveMediator.ferdigstill(any(), IDENT, OID) }
    }

    @Test
    fun `løser godkjenningsbehov ved avvist utbetaling`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns godkjenningsbehov
        val saksbehandlerløsning = saksbehandlerløsning(false)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(false)
        verify(exactly = 1) { oppgaveMediator.ferdigstill(any(), IDENT, OID) }
    }

    private fun assertLøsning(godkjent: Boolean) {
        context.meldinger().also { meldinger ->
            val løsning = assertNotNull(meldinger
                .map(objectMapper::readTree)
                .filter { it["@event_name"].asText() == "behov" }
                .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") })
            assertJsonEquals(GODKJENNINGSBEHOV_JSON, løsning)
            val godkjenning = løsning.path("@løsning").path("Godkjenning")
            assertTrue(godkjenning.path("godkjent").isBoolean)
            assertEquals(godkjent, godkjenning.path("godkjent").booleanValue())
            assertEquals(IDENT, godkjenning.path("saksbehandlerIdent").textValue())
            assertEquals(GODKJENTTIDSPUNKT, LocalDateTime.parse(godkjenning.path("godkjenttidspunkt").textValue()))
            assertTrue(godkjenning.path("årsak").isNull)
            assertTrue(godkjenning.path("kommentar").isNull)
            assertTrue(godkjenning.path("begrunnelser").isNull)
        }
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
