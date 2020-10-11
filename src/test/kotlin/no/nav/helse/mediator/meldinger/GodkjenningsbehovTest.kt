package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class GodkjenningsbehovTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val HENDELSE_JSON = """{ "this_key_should_exist": "this_value_should_exist" }"""
        private val objectMapper = jacksonObjectMapper()
    }

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
        hendelseDao = mockk(),
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
        miljøstyrtFeatureToggle = mockk(relaxed = true),
        automatisering = mockk(relaxed = true)
    )
    private val godkjenningMessage = hendelsefabrikk.godkjenning(
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
        context.add(HentPersoninfoløsning("Kari", null, "Nordmann", LocalDate.EPOCH, Kjønn.Kvinne))
        context.add(HentEnhetløsning("3101"))
        context.add(HentInfotrygdutbetalingerløsning(objectMapper.createObjectNode()))

        assertFalse(godkjenningMessage.execute(context))

        context.add(DigitalKontaktinformasjonløsning(LocalDateTime.now(), FNR, true))
        assertFalse(godkjenningMessage.resume(context))

        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(godkjenningMessage.resume(context))

        assertEquals(listOf("DigitalKontaktinformasjon", "ÅpneOppgaver"), context.behov().keys.toList())
        verify(exactly = 1) { oppgaveMediator.nyOppgave(any()) }
    }

    @Test
    fun `lager oppgave med tildeling`() {
        val reservasjon = Pair(UUID.randomUUID(), LocalDateTime.now())

        every { personDao.findPersonByFødselsnummer(FNR) } returnsMany listOf(null, 1)
        every { arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR) } returnsMany listOf(1)
        every { reservasjonDao.hentReservasjonFor(FNR) } returns reservasjon
        context.add(HentPersoninfoløsning("Kari", null, "Nordmann", LocalDate.EPOCH, Kjønn.Kvinne))
        context.add(HentEnhetløsning("3101"))
        context.add(HentInfotrygdutbetalingerløsning(objectMapper.createObjectNode()))

        assertFalse(godkjenningMessage.execute(context))

        context.add(DigitalKontaktinformasjonløsning(LocalDateTime.now(), FNR, true))
        assertFalse(godkjenningMessage.resume(context))

        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(godkjenningMessage.resume(context))

        assertEquals(listOf("DigitalKontaktinformasjon", "ÅpneOppgaver"), context.behov().keys.toList())
        verify(exactly = 1) { oppgaveMediator.tildel(any(), reservasjon.first, reservasjon.second) }
    }

    private fun personFinnesIkke() {
        every { personDao.findPersonByFødselsnummer(FNR) } returns null
    }
}
