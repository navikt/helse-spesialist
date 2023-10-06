package no.nav.helse.mediator

import TilgangskontrollForTestHarIkkeTilgang
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.EnumSet
import java.util.UUID
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.idForGruppe
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.OVERGANG_FRA_IT
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveInspector.Companion.oppgaveinspektør
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.Int.Companion.MAX_VALUE
import kotlin.random.Random.Default.nextLong

internal class OppgaveMediatorTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val UTBETALING_ID_2 = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private val COMMAND_CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestHendelse(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
        private val OPPGAVE_ID = nextLong()
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLERNAVN = "Hen Saksbehandler"
        private const val OPPGAVETYPE_SØKNAD = "SØKNAD"
        private val EGENSKAPER = listOf(SØKNAD, UTBETALING_TIL_SYKMELDT, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val hendelseDao = mockk<HendelseDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val saksbehandlerDao = mockk<SaksbehandlerDao>()
    private val testRapid = TestRapid()

    private val mediator = OppgaveMediator(
        hendelseDao = hendelseDao,
        oppgaveDao = oppgaveDao,
        tildelingDao = tildelingDao,
        reservasjonDao = reservasjonDao,
        opptegnelseDao = opptegnelseDao,
        totrinnsvurderingRepository = totrinnsvurderingDao,
        saksbehandlerRepository = saksbehandlerDao,
        rapidsConnection = testRapid,
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        tilgangsgrupper = Tilgangsgrupper(testEnv)
    )
    private val saksbehandlerFraDatabase = SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT)
    private fun saksbehandlerFraApi(tilganger: List<UUID> = emptyList()) = SaksbehandlerFraApi(SAKSBEHANDLEROID, SAKSBEHANDLEREPOST, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT, tilganger)
    private val saksbehandler = Saksbehandler(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT, TilgangskontrollForTestHarIkkeTilgang)
    private fun søknadsoppgave(id: Long): Oppgave = Oppgave.nyOppgave(id, VEDTAKSPERIODE_ID, UTBETALING_ID, HENDELSE_ID, listOf(SØKNAD))
    private fun stikkprøveoppgave(id: Long): Oppgave = Oppgave.nyOppgave(id, VEDTAKSPERIODE_ID_2, UTBETALING_ID_2, UUID.randomUUID(), listOf(STIKKPRØVE))
    private fun riskoppgave(id: Long): Oppgave = Oppgave.nyOppgave(id, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), listOf(RISK_QA))

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao, tildelingDao, opptegnelseDao)
        testRapid.reset()
    }

    @Test
    fun `lagrer oppgaver`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        mediator.nyOppgave(TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID) {
            søknadsoppgave(it)
        }
        verify(exactly = 1) {
            oppgaveDao.opprettOppgave(
                0L,
                COMMAND_CONTEXT_ID,
                OPPGAVETYPE_SØKNAD,
                listOf(OPPGAVETYPE_SØKNAD),
                VEDTAKSPERIODE_ID,
                UTBETALING_ID
            )
        }
        assertEquals(1, testRapid.inspektør.size)
        assertOppgaveevent(0, "oppgave_opprettet")
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `lagrer oppgave og tildeler til saksbehandler som har reservert personen`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjon(saksbehandlerFraDatabase, false)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        lateinit var oppgave: Oppgave
        mediator.nyOppgave(TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID) {
            søknadsoppgave(it).also { søknadsoppgave -> oppgave = søknadsoppgave }
        }

        oppgaveinspektør(oppgave) {
            assertEquals(saksbehandler, tildeltTil)
        }
        verify(exactly = 1) { tildelingDao.tildel(any(), SAKSBEHANDLEROID, any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `tildeler ikke risk-oppgave til saksbehandler som har reservert personen hvis hen ikke har risk-tilgang`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjon(saksbehandlerFraDatabase, false)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        lateinit var oppgave: Oppgave
        mediator.nyOppgave(TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID) {
            riskoppgave(it).also { riskoppgave -> oppgave = riskoppgave }
        }

        oppgaveinspektør(oppgave) {
            assertEquals(null, tildeltTil)
        }
        verify(exactly = 0) { tildelingDao.tildel(any(), SAKSBEHANDLEROID, any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `tildeler ikke reservert personen når oppgave er stikkprøve`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjon(saksbehandlerFraDatabase, false)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.nyOppgave(TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID) {
            stikkprøveoppgave(it)
        }
        verify(exactly = 0) { tildelingDao.tildel(any(), any(), any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `kaller bare hentGrupper når personen er reservert`() {
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        lateinit var oppgave: Oppgave
        mediator.nyOppgave(TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID) {
            stikkprøveoppgave(it).also { stikkprøveoppgave -> oppgave = stikkprøveoppgave }
        }

        oppgaveinspektør(oppgave) {
            assertEquals(null, tildeltTil)
        }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `Legg oppgave på vent`() {
        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(1L) } returns null
        every { oppgaveDao.finnOppgave(1L) } returns oppgaveFraDatabase(1L, tildelt = true)
        val tildeling = mediator.leggPåVent(1L)
        assertEquals(true, tildeling.påVent)
        assertEquals(SAKSBEHANDLEROID, tildeling.oid)
        assertEquals(SAKSBEHANDLERNAVN, tildeling.navn)
        assertEquals(SAKSBEHANDLEREPOST, tildeling.epost)
    }

    @Test
    fun `Fjern oppgave fra på vent`() {
        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(1L) } returns null
        every { oppgaveDao.finnOppgave(1L) } returns oppgaveFraDatabase(1L, tildelt = true)
        mediator.leggPåVent(1L)
        val tildeling = mediator.fjernPåVent(1L)
        assertEquals(false, tildeling.påVent)
        assertEquals(SAKSBEHANDLEROID, tildeling.oid)
        assertEquals(SAKSBEHANDLERNAVN, tildeling.navn)
        assertEquals(SAKSBEHANDLEREPOST, tildeling.epost)
    }

    @Test
    fun `oppdaterer oppgave`() {
        every { oppgaveDao.finnOppgave(OPPGAVE_ID) } returns oppgaveFraDatabase()
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { saksbehandlerDao.finnSaksbehandler(any()) } returns saksbehandlerFraDatabase
        mediator.oppgave(OPPGAVE_ID) {
            avventerSystem(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
            ferdigstill()
        }
        assertEquals(2, testRapid.inspektør.size)
        assertOppgaveevent(1, "oppgave_oppdatert", Oppgavestatus.Ferdigstilt) {
            assertEquals(OPPGAVE_ID, it.path("oppgaveId").longValue())
            assertEquals(SAKSBEHANDLERIDENT, it.path("ferdigstiltAvIdent").asText())
            assertEquals(SAKSBEHANDLEROID, UUID.fromString(it.path("ferdigstiltAvOid").asText()))
        }
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `lagrer ikke dobbelt`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { oppgaveDao.opprettOppgave(any(), any(), OPPGAVETYPE_SØKNAD, listOf(OPPGAVETYPE_SØKNAD), any(), any()) } returns 0L
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null

        mediator.nyOppgave(TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID) {
            søknadsoppgave(it)
        }
        assertEquals(1, testRapid.inspektør.size)
        assertAntallOpptegnelser(1)
        testRapid.reset()
        clearMocks(opptegnelseDao)
        assertEquals(0, testRapid.inspektør.size)
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `Hent oppgaver til visning`() {
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns listOf(
            oppgaveFraDatabaseForVisning(),
            oppgaveFraDatabaseForVisning(),
        )
        val oppgaver = mediator.oppgaver(saksbehandlerFraApi(), 0, MAX_VALUE, emptyList())
        assertEquals(2, oppgaver.size)
    }

    @Test
    fun `Hent kun oppgaver til visning som saksbehandler har tilgang til`() {
        mediator.oppgaver(saksbehandlerFraApi(), 0, MAX_VALUE, emptyList())
        verify(exactly = 1) { oppgaveDao.finnOppgaverForVisning(
            ekskluderEgenskaper = Egenskap.alleTilgangsstyrteEgenskaper.map { it.name },
            SAKSBEHANDLEROID,
            0,
            MAX_VALUE
        ) }
    }

    @Test
    fun `Mapper oppgave til visning riktig`() {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        every { oppgaveDao.finnOppgaverForVisning(emptyList(), any()) } returns listOf(
            oppgaveFraDatabaseForVisning(
                oppgaveId = 1L,
                aktørId = "1234567891011",
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                vedtaksperiodeId = vedtaksperiodeId,
                personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                tildelt = SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT),
                påVent = true,
            ),
        )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList())
        assertEquals(1, oppgaver.size)
        val oppgave = oppgaver.single()
        assertEquals("1", oppgave.id)
        assertEquals(opprettet.toString(), oppgave.opprettet)
        assertEquals(opprinneligSøknadsdato.toString(), oppgave.opprinneligSoknadsdato)
        assertEquals(vedtaksperiodeId.toString(), oppgave.vedtaksperiodeId)
        assertEquals("fornavn", oppgave.navn.fornavn)
        assertEquals("mellomnavn", oppgave.navn.mellomnavn)
        assertEquals("etternavn", oppgave.navn.etternavn)
        assertEquals("1234567891011", oppgave.aktorId)
        assertEquals(SAKSBEHANDLERNAVN, oppgave.tildeling?.navn)
        assertEquals(SAKSBEHANDLEREPOST, oppgave.tildeling?.epost)
        assertEquals(SAKSBEHANDLEROID.toString(), oppgave.tildeling?.oid)
        assertEquals(true, oppgave.tildeling?.paaVent)
        assertEquals(EGENSKAPER.size, oppgave.egenskaper.size)
    }

    @ParameterizedTest
    @EnumSource(names = ["REVURDERING", "SØKNAD"], mode = EnumSource.Mode.INCLUDE)
    fun `Mapper oppgavetypeegenskaper riktig`(egenskap: Egenskap) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper = listOf(egenskap, UTBETALING_TIL_SYKMELDT, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
        every { oppgaveDao.finnOppgaverForVisning(emptyList(), any()) } returns listOf(
            oppgaveFraDatabaseForVisning(
                oppgaveId = 1L,
                aktørId = "1234567891011",
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                vedtaksperiodeId = vedtaksperiodeId,
                personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                tildelt = SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT),
                påVent = true,
                egenskaper = egenskaper.map { it.name },
            ),
        )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList())
        val oppgave = oppgaver.single()
        assertEquals(egenskap.oppgavetype(), oppgave.oppgavetype)
    }

    @ParameterizedTest
    @EnumSource(names = ["FORLENGELSE", "INFOTRYGDFORLENGELSE", "OVERGANG_FRA_IT", "FORSTEGANGSBEHANDLING"], mode = EnumSource.Mode.INCLUDE)
    fun `Mapper periodetypeegenskaper riktig`(egenskap: Egenskap) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper = listOf(egenskap, UTBETALING_TIL_SYKMELDT, EN_ARBEIDSGIVER, SØKNAD)
        every { oppgaveDao.finnOppgaverForVisning(emptyList(), any()) } returns listOf(
            oppgaveFraDatabaseForVisning(
                oppgaveId = 1L,
                aktørId = "1234567891011",
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                vedtaksperiodeId = vedtaksperiodeId,
                personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                tildelt = SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT),
                påVent = true,
                egenskaper = egenskaper.map { it.name },
            ),
        )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList())
        val oppgave = oppgaver.single()
        assertEquals(egenskap.periodetype(), oppgave.periodetype)
    }

    @ParameterizedTest
    @EnumSource(names = ["UTBETALING_TIL_SYKMELDT", "DELVIS_REFUSJON", "UTBETALING_TIL_ARBEIDSGIVER", "INGEN_UTBETALING"], mode = EnumSource.Mode.INCLUDE)
    fun `Mapper mottakeregenskaper riktig`(egenskap: Egenskap) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper = listOf(egenskap, FORSTEGANGSBEHANDLING, EN_ARBEIDSGIVER, SØKNAD)
        every { oppgaveDao.finnOppgaverForVisning(emptyList(), any()) } returns listOf(
            oppgaveFraDatabaseForVisning(
                oppgaveId = 1L,
                aktørId = "1234567891011",
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                vedtaksperiodeId = vedtaksperiodeId,
                personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                tildelt = SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT),
                påVent = true,
                egenskaper = egenskaper.map { it.name },
            ),
        )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList())
        val oppgave = oppgaver.single()
        assertEquals(egenskap.mottaker(), oppgave.mottaker)
    }

    @ParameterizedTest
    @EnumSource(names = ["EN_ARBEIDSGIVER", "FLERE_ARBEIDSGIVERE"], mode = EnumSource.Mode.INCLUDE)
    fun `Mapper inntektskildegenskaper riktig`(egenskap: Egenskap) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper = listOf(egenskap, FORSTEGANGSBEHANDLING, UTBETALING_TIL_SYKMELDT, SØKNAD)
        every { oppgaveDao.finnOppgaverForVisning(emptyList(), any()) } returns listOf(
            oppgaveFraDatabaseForVisning(
                oppgaveId = 1L,
                aktørId = "1234567891011",
                opprettet = opprettet,
                opprinneligSøknadsdato = opprinneligSøknadsdato,
                vedtaksperiodeId = vedtaksperiodeId,
                personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                tildelt = SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT),
                påVent = true,
                egenskaper = egenskaper.map { it.name },
            ),
        )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList())
        val oppgave = oppgaver.single()
        assertEquals(egenskap.antallArbeidsforhold(), oppgave.antallArbeidsforhold)
    }

    private fun assertAntallOpptegnelser(antallOpptegnelser: Int) = verify(exactly = antallOpptegnelser) {
        opptegnelseDao.opprettOpptegnelse(
            eq(TESTHENDELSE.fødselsnummer()),
            any(),
            eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)
        )
    }

    private fun assertOpptegnelseIkkeOpprettet() = assertAntallOpptegnelser(0)

    private fun assertOppgaveevent(
        indeks: Int,
        navn: String,
        status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
        assertBlock: (JsonNode) -> Unit = {},
    ) {
        testRapid.inspektør.message(indeks).also {
            assertEquals(navn, it.path("@event_name").asText())
            assertEquals(HENDELSE_ID, UUID.fromString(it.path("hendelseId").asText()))
            assertEquals(status, enumValueOf<Oppgavestatus>(it.path("status").asText()))
            assertTrue(it.hasNonNull("oppgaveId"))
            assertBlock(it)
        }
    }

    private fun oppgaveFraDatabaseForVisning(
        oppgaveId: Long = nextLong(),
        egenskaper: List<String> = EGENSKAPER.map { it.name },
        aktørId: String = nextLong(1000000000000, 2000000000000).toString(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        opprinneligSøknadsdato: LocalDateTime = LocalDateTime.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        personnavnFraDatabase: PersonnavnFraDatabase = PersonnavnFraDatabase("navn", "mellomnavn", "etternavn"),
        tildelt: SaksbehandlerFraDatabase? = null,
        påVent: Boolean = false
    ) =
        OppgaveFraDatabaseForVisning(
            id = oppgaveId,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            navn = personnavnFraDatabase,
            egenskaper = egenskaper,
            tildelt = tildelt,
            påVent = påVent,
            opprettet = opprettet,
            opprinneligSøknadsdato = opprinneligSøknadsdato,
        )

    private fun oppgaveFraDatabase(oppgaveId: Long = OPPGAVE_ID, tildelt: Boolean = false) = OppgaveFraDatabase(
        id = oppgaveId,
        egenskap = "SØKNAD",
        egenskaper = listOf("SØKNAD"),
        status = "AvventerSaksbehandler",
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        utbetalingId = UTBETALING_ID,
        hendelseId = HENDELSE_ID,
        ferdigstiltAvIdent = null,
        ferdigstiltAvOid = null,
        tildelt = if (tildelt) SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT) else null,
        påVent = false
    )

    private fun Egenskap.periodetype(): Periodetype = when (this) {
        FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
        FORLENGELSE -> Periodetype.FORLENGELSE
        INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
        OVERGANG_FRA_IT -> Periodetype.OVERGANG_FRA_IT
        else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
    }

    private fun Egenskap.oppgavetype(): Oppgavetype = when (this) {
        SØKNAD -> Oppgavetype.SOKNAD
        REVURDERING -> Oppgavetype.REVURDERING
        STIKKPRØVE -> Oppgavetype.STIKKPROVE
        else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
    }

    private fun Egenskap.mottaker(): Mottaker = when (this) {
        UTBETALING_TIL_SYKMELDT -> Mottaker.SYKMELDT
        UTBETALING_TIL_ARBEIDSGIVER -> Mottaker.ARBEIDSGIVER
        DELVIS_REFUSJON -> Mottaker.BEGGE
        INGEN_UTBETALING -> Mottaker.INGEN
        else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
    }

    private fun Egenskap.antallArbeidsforhold(): AntallArbeidsforhold = when (this) {
        EN_ARBEIDSGIVER -> AntallArbeidsforhold.ET_ARBEIDSFORHOLD
        FLERE_ARBEIDSGIVERE -> AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
        else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
    }
}
