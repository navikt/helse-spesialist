package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.mediator.kafka.Hendelsefabrikk
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.HendelseDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.tildeling.ReservasjonDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerløsningMessageTest {

    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val CONTEXT_ID = UUID.randomUUID()
        private val OID = UUID.randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private const val OPPGAVE_ID = 1L
        private const val FNR = "12020052345"
        private const val IDENT = "Z999999"
        private const val HENDELSE_JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
        private const val GODKJENNINGSBEHOV_JSON = """{ "foo": "bar" }"""
        private const val SAKSBEHANDLER = "Sak Saksen"
        private val objectMapper = jacksonObjectMapper()
    }

    private val hendelseDao = mockk<HendelseDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val arbeidsgiverDao = mockk<ArbeidsgiverDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
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
        miljøstyrtFeatureToggle = mockk(relaxed = true),
        automatisering = mockk(relaxed = true)
    )

    private val godkjenningsbehov = UtbetalingsgodkjenningMessage(GODKJENNINGSBEHOV_JSON)
    private fun saksbehandlerløsning(godkjent: Boolean) = hendelsefabrikk.saksbehandlerløsning(
        id = HENDELSE_ID,
        godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
        contextId = CONTEXT_ID,
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
        verify(exactly = 1) { oppgaveMediator.ferdigstill(any(), OPPGAVE_ID, IDENT, OID) }
    }

    @Test
    fun `løser godkjenningsbehov ved avvist utbetaling`() {
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns godkjenningsbehov
        val saksbehandlerløsning = saksbehandlerløsning(false)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(false)
        verify(exactly = 1) { oppgaveMediator.ferdigstill(any(), OPPGAVE_ID, IDENT, OID) }
    }

    private fun assertLøsning(godkjent: Boolean) {
        context.meldinger().also { meldinger ->
            assertEquals(1, meldinger.size)
            assertJsonEquals(GODKJENNINGSBEHOV_JSON, meldinger.first())
            objectMapper.readTree(meldinger.first()).also { json ->
                val løsning = json.path("@løsning").path("Godkjenning")
                assertTrue(løsning.path("godkjent").isBoolean)
                assertEquals(godkjent, løsning.path("godkjent").booleanValue())
                assertEquals(IDENT, løsning.path("saksbehandlerIdent").textValue())
                assertEquals(GODKJENTTIDSPUNKT, LocalDateTime.parse(løsning.path("godkjenttidspunkt").textValue()))
                assertTrue(løsning.path("årsak").isNull)
                assertTrue(løsning.path("kommentar").isNull)
                assertTrue(løsning.path("begrunnelser").isNull)
            }
        }
    }

    private fun assertJsonEquals(expected: String, actual: String) {
        val expectedJson = objectMapper.readTree(expected)
        val actualJson = objectMapper.readTree(actual)
        assertJsonEquals(expectedJson, actualJson)
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
