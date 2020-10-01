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
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.person.*
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.tildeling.ReservasjonDao
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class NyGodkjenningMessageTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val HENDELSE_JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
        private const val SAKSBEHANDLER = "Sak Saksen"
        private val objectMapper = jacksonObjectMapper()
    }

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
        digitalKontaktinformasjonDao = mockk(),
        miljøstyrtFeatureToggle = mockk(relaxed = true),
        automatisering = mockk(relaxed = true)
    )
    private val godkjenningMessage = hendelsefabrikk.nyGodkjenning(
        id = HENDELSE_ID,
        fødselsnummer = FNR,
        aktørId = AKTØR,
        organisasjonsnummer = ORGNR,
        periodeFom = LocalDate.MIN,
        periodeTom = LocalDate.MAX,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        warnings = emptyList(),
        periodetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING,
        json = HENDELSE_JSON
    )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `etterspør nødvendig informasjon`() {
        personFinnesIkke()
        assertFalse(godkjenningMessage.execute(context))
        assertEquals(listOf("HentPersoninfo", "HentEnhet", "HentInfotrygdutbetalinger"), context.behov().keys.toList())
    }

    @Test
    fun `lager oppgave`() {
        every { personDao.findPersonByFødselsnummer(FNR) } returnsMany listOf(null, 1)
        every { arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR) } returnsMany listOf(1)
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        context.add(HentPersoninfoLøsning("Kari", null, "Nordmann", LocalDate.EPOCH, Kjønn.Kvinne))
        context.add(HentEnhetLøsning("3101"))
        context.add(HentInfotrygdutbetalingerLøsning(objectMapper.createObjectNode()))

        assertFalse(godkjenningMessage.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 1) { oppgaveMediator.oppgave(any(), any()) }
    }

    @Test
    fun `lager oppgave med tildeling`() {
        val reservasjon = Pair(UUID.randomUUID(), LocalDateTime.now())

        every { personDao.findPersonByFødselsnummer(FNR) } returnsMany listOf(null, 1)
        every { arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR) } returnsMany listOf(1)
        every { reservasjonDao.hentReservasjonFor(FNR) } returns reservasjon
        context.add(HentPersoninfoLøsning("Kari", null, "Nordmann", LocalDate.EPOCH, Kjønn.Kvinne))
        context.add(HentEnhetLøsning("3101"))
        context.add(HentInfotrygdutbetalingerLøsning(objectMapper.createObjectNode()))

        assertFalse(godkjenningMessage.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 1) { oppgaveMediator.oppgave(any(), reservasjon) }
    }

    @Test
    fun `løser godkjenningsbehov`() {
        val godkjenttidspunkt = LocalDateTime.now()
        every { personDao.findPersonByFødselsnummer(FNR) } returnsMany listOf(null, 1)
        every { arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR) } returnsMany listOf(1)
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        context.add(HentPersoninfoLøsning("Kari", null, "Nordmann", LocalDate.EPOCH, Kjønn.Kvinne))
        context.add(HentEnhetLøsning("3101"))
        context.add(HentInfotrygdutbetalingerLøsning(objectMapper.createObjectNode()))
        context.add(
            SaksbehandlerLøsning(
                godkjent = true,
                saksbehandlerIdent = SAKSBEHANDLER,
                oid = UUID.randomUUID(),
                epostadresse = "saksbehandler@nav.no",
                godkjenttidspunkt = godkjenttidspunkt,
                årsak = null,
                begrunnelser = emptyList(),
                kommentar = null,
                oppgaveId = 1L
            )
        )

        assertTrue(godkjenningMessage.execute(context))
        assertFalse(context.harBehov())

        context.meldinger().also { meldinger ->
            assertEquals(1, meldinger.size)
            assertJsonEquals(HENDELSE_JSON, meldinger.first())
            objectMapper.readTree(meldinger.first()).also { json ->
                val løsning = json.path("@løsning").path("Godkjenning")
                assertTrue(løsning.path("godkjent").booleanValue())
                assertEquals(SAKSBEHANDLER, løsning.path("saksbehandlerIdent").textValue())
                assertEquals(godkjenttidspunkt, LocalDateTime.parse(løsning.path("godkjenttidspunkt").textValue()))
                assertTrue(løsning.path("årsak").isNull)
                assertTrue(løsning.path("kommentar").isNull)
                assertTrue(løsning.path("begrunnelser").isArray)
            }
        }
    }

    private fun personFinnesIkke() {
        every { personDao.findPersonByFødselsnummer(FNR) } returns null
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
