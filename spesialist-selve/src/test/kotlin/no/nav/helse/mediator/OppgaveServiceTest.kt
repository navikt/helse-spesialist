package no.nav.helse.mediator

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OpptegnelseRepository
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonRepository
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TildelingRepository
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.TestMelding
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Egenskap.PA_VENT
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.test.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import no.nav.helse.spesialist.test.lagSaksbehandlernavn
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.util.idForGruppe
import no.nav.helse.util.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.EnumSet
import java.util.UUID
import kotlin.Int.Companion.MAX_VALUE
import kotlin.random.Random.Default.nextLong

internal class OppgaveServiceTest {
    private val FNR = lagFødselsnummer()
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
    private val BEHANDLING_ID = UUID.randomUUID()
    private val BEHANDLING_ID_2 = UUID.randomUUID()
    private val UTBETALING_ID = UUID.randomUUID()
    private val UTBETALING_ID_2 = UUID.randomUUID()
    private val HENDELSE_ID = UUID.randomUUID()
    private val TESTHENDELSE = TestMelding(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
    private val OPPGAVE_ID = nextLong()
    private val SAKSBEHANDLERIDENT = lagSaksbehandlerident()
    private val SAKSBEHANDLEROID = UUID.randomUUID()
    private val SAKSBEHANDLERNAVN = lagSaksbehandlernavn()
    private val SAKSBEHANDLEREPOST = lagEpostadresseFraFulltNavn(SAKSBEHANDLERNAVN)
    private val EGENSKAP_SØKNAD = EgenskapForDatabase.SØKNAD
    private val EGENSKAPER =
        setOf(
            EgenskapForDatabase.SØKNAD,
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
            EgenskapForDatabase.EN_ARBEIDSGIVER,
            EgenskapForDatabase.FORSTEGANGSBEHANDLING,
        )

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val tildelingRepository = mockk<TildelingRepository>(relaxed = true)
    private val reservasjonRepository = mockk<ReservasjonRepository>(relaxed = true)
    private val opptegnelseRepository = mockk<OpptegnelseRepository>(relaxed = true)
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val saksbehandlerRepository = mockk<SaksbehandlerRepository>()

    private val meldingPubliserer = object : MeldingPubliserer {
        var antallMeldinger: Int = 0
            private set

        override fun publiser(fødselsnummer: String, hendelse: UtgåendeHendelse, årsak: String) {
            antallMeldinger++
        }

        override fun publiser(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent, versjonAvKode: String) =
            error("Not implemented for test")

        override fun publiser(
            hendelseId: UUID,
            commandContextId: UUID,
            fødselsnummer: String,
            behov: List<Behov>
        ) = error("Not implemented for test")

        override fun publiser(event: KommandokjedeEndretEvent, hendelseNavn: String) = error("Not implemented for test")
    }

    private val mediator =
        OppgaveService(
            oppgaveDao = oppgaveDao,
            tildelingRepository = tildelingRepository,
            reservasjonRepository = reservasjonRepository,
            opptegnelseRepository = opptegnelseRepository,
            totrinnsvurderingDao = totrinnsvurderingDao,
            saksbehandlerRepository = saksbehandlerRepository,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = SpeilTilgangsgrupper(testEnv),
        )
    private val saksbehandlerFraDatabase =
        SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT)

    private fun saksbehandlerFraApi(tilganger: List<UUID> = emptyList()) =
        SaksbehandlerFraApi(SAKSBEHANDLEROID, SAKSBEHANDLEREPOST, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT, tilganger)

    private fun lagSøknadsoppgave(
        fødselsnummer: String,
    ) {
        mediator.nyOppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            behandlingId = BEHANDLING_ID,
            utbetalingId = UTBETALING_ID,
            hendelseId = HENDELSE_ID,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD),
        )
    }

    private fun lagStikkprøveoppgave(
        fødselsnummer: String,
    ) {
        mediator.nyOppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            behandlingId = BEHANDLING_ID_2,
            utbetalingId = UTBETALING_ID_2,
            hendelseId = HENDELSE_ID,
            kanAvvises = true,
            egenskaper = setOf(STIKKPRØVE),
        )
    }

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, tildelingRepository, opptegnelseRepository)
    }

    @Test
    fun `lagrer oppgaver`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        every { reservasjonRepository.hentReservasjonFor(fødselsnummer) } returns null
        lagSøknadsoppgave(fødselsnummer)
        verify(exactly = 1) {
            oppgaveDao.opprettOppgave(
                id = oppgaveId,
                godkjenningsbehovId = HENDELSE_ID,
                egenskaper = listOf(EGENSKAP_SØKNAD),
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                kanAvvises = true,
            )
        }
        assertEquals(1, meldingPubliserer.antallMeldinger)
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `lagrer oppgave og tildeler til saksbehandler som har reservert personen`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        val fødselsnummer = lagFødselsnummer()
        every { reservasjonRepository.hentReservasjonFor(fødselsnummer) } returns Reservasjon(saksbehandlerFraDatabase)
        every { oppgaveDao.finnFødselsnummer(0L) } returns fødselsnummer
        lagSøknadsoppgave(fødselsnummer)
        verify(exactly = 1) { tildelingRepository.tildel(0L, SAKSBEHANDLEROID) }
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `tildeler ikke reservert personen når oppgave er stikkprøve`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { reservasjonRepository.hentReservasjonFor(fødselsnummer) } returns Reservasjon(saksbehandlerFraDatabase)
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        lagStikkprøveoppgave(fødselsnummer)
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `kaller bare hentGrupper når personen er reservert`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { reservasjonRepository.hentReservasjonFor(fødselsnummer) } returns null
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        lagStikkprøveoppgave(fødselsnummer)
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `oppdaterer oppgave`() {
        every { oppgaveDao.finnOppgave(OPPGAVE_ID) } returns oppgaveFraDatabase()
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { saksbehandlerRepository.finnSaksbehandler(any()) } returns saksbehandlerFraDatabase
        mediator.oppgave(OPPGAVE_ID) {
            avventerSystem(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
            ferdigstill()
        }
        assertEquals(2, meldingPubliserer.antallMeldinger)
        assertOpptegnelseIkkeOpprettet(TESTHENDELSE.fødselsnummer())
    }

    @Test
    fun `lagrer ikke dobbelt`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        every { reservasjonRepository.hentReservasjonFor(fødselsnummer) } returns null

        lagSøknadsoppgave(fødselsnummer)

        assertEquals(1, meldingPubliserer.antallMeldinger)
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `Hent oppgaver til visning`() {
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(filtrertAntall = 2),
                    oppgaveFraDatabaseForVisning(filtrertAntall = 2),
                )
        val oppgaver = mediator.oppgaver(saksbehandlerFraApi(), 0, MAX_VALUE, emptyList(), Filtrering())
        assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent antall mine saker og mine saker på vent til visning`() {
        every { oppgaveDao.finnAntallOppgaver(any()) } returns AntallOppgaverFraDatabase(
            antallMineSaker = 2,
            antallMineSakerPåVent = 1
        )
        val antallOppgaver = mediator.antallOppgaver(saksbehandlerFraApi())
        assertEquals(2, antallOppgaver.antallMineSaker)
        assertEquals(1, antallOppgaver.antallMineSakerPaVent)
    }

    @Test
    fun `Hent behandlede oppgaver til visning`() {
        every { oppgaveDao.finnBehandledeOppgaver(any()) } returns
                listOf(
                    behandletOppgaveFraDatabaseForVisning(filtrertAntall = 2),
                    behandletOppgaveFraDatabaseForVisning(filtrertAntall = 2),
                )
        val oppgaver = mediator.behandledeOppgaver(saksbehandlerFraApi(), 0, MAX_VALUE)
        assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent kun oppgaver til visning som saksbehandler har tilgang til`() {
        mediator.oppgaver(saksbehandlerFraApi(), 0, MAX_VALUE, emptyList(), Filtrering())
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = Egenskap.alleTilgangsstyrteEgenskaper.map { it.name },
                SAKSBEHANDLEROID,
                0,
                MAX_VALUE,
            )
        }
    }

    @Test
    fun `Ekskluderer alle ukategoriserte egenskaper hvis ingenUkategoriserteEgenskaper i Filtrering er satt til true`() {
        mediator.oppgaver(
            saksbehandlerFraApi = saksbehandlerFraApi(),
            offset = 0,
            limit = MAX_VALUE,
            sortering = emptyList(),
            filtrering = Filtrering(ingenUkategoriserteEgenskaper = true)
        )
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper =
                    Egenskap.alleTilgangsstyrteEgenskaper.map { it.name } + Egenskap.alleUkategoriserteEgenskaper.map { it.name },
                SAKSBEHANDLEROID,
                0,
                MAX_VALUE,
            )
        }
    }

    @Test
    fun `Ekskluderer alle ekskluderteEgenskaper`() {
        mediator.oppgaver(
            saksbehandlerFraApi(),
            0,
            MAX_VALUE,
            emptyList(),
            Filtrering(
                ekskluderteEgenskaper =
                    listOf(
                        Oppgaveegenskap(
                            egenskap = PA_VENT,
                            kategori = Kategori.Status,
                        ),
                    ),
            ),
        )
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = Egenskap.alleTilgangsstyrteEgenskaper.map { it.name } + Egenskap.PÅ_VENT.name,
                SAKSBEHANDLEROID,
                0,
                MAX_VALUE,
            )
        }
    }

    @Test
    fun `Mapper behandlet oppgave til visning riktig`() {
        val ferdigstiltTidspunkt = LocalDateTime.now()
        every { oppgaveDao.finnBehandledeOppgaver(any()) } returns
                listOf(
                    behandletOppgaveFraDatabaseForVisning(
                        oppgaveId = 1L,
                        aktørId = "1234567891011",
                        personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                        ferdigstiltAv = "Kurt",
                        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi()
        val oppgaver = mediator.behandledeOppgaver(saksbehandler, 0, MAX_VALUE)
        assertEquals(1, oppgaver.oppgaver.size)
        val oppgave = oppgaver.oppgaver.single()
        assertEquals("1", oppgave.id)
        assertEquals("1234567891011", oppgave.aktorId)
        assertEquals("fornavn", oppgave.personnavn.fornavn)
        assertEquals("mellomnavn", oppgave.personnavn.mellomnavn)
        assertEquals("etternavn", oppgave.personnavn.etternavn)
        assertEquals("Kurt", oppgave.ferdigstiltAv)
        assertEquals(ferdigstiltTidspunkt, oppgave.ferdigstiltTidspunkt)
        assertEquals(Oppgavetype.SOKNAD, oppgave.oppgavetype)
        assertEquals(Periodetype.FORSTEGANGSBEHANDLING, oppgave.periodetype)
        assertEquals(AntallArbeidsforhold.ET_ARBEIDSFORHOLD, oppgave.antallArbeidsforhold)
    }

    @Test
    fun `Mapper oppgave til visning riktig`() {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(
                        oppgaveId = 1L,
                        aktørId = "1234567891011",
                        opprettet = opprettet,
                        opprinneligSøknadsdato = opprinneligSøknadsdato,
                        vedtaksperiodeId = vedtaksperiodeId,
                        personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                        tildelt = SaksbehandlerFraDatabase(
                            SAKSBEHANDLEREPOST,
                            SAKSBEHANDLEROID,
                            SAKSBEHANDLERNAVN,
                            SAKSBEHANDLERIDENT
                        ),
                        påVent = true,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map {
            UUID.fromString(
                idForGruppe(it)
            )
        })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList(), Filtrering())
        assertEquals(1, oppgaver.oppgaver.size)
        val oppgave = oppgaver.oppgaver.single()
        assertEquals("1", oppgave.id)
        assertEquals(opprettet, oppgave.opprettet)
        assertEquals(opprinneligSøknadsdato, oppgave.opprinneligSoknadsdato)
        assertEquals(vedtaksperiodeId, oppgave.vedtaksperiodeId)
        assertEquals("fornavn", oppgave.navn.fornavn)
        assertEquals("mellomnavn", oppgave.navn.mellomnavn)
        assertEquals("etternavn", oppgave.navn.etternavn)
        assertEquals("1234567891011", oppgave.aktorId)
        assertEquals(SAKSBEHANDLERNAVN, oppgave.tildeling?.navn)
        assertEquals(SAKSBEHANDLEREPOST, oppgave.tildeling?.epost)
        assertEquals(SAKSBEHANDLEROID, oppgave.tildeling?.oid)
        assertEquals(EGENSKAPER.size, oppgave.egenskaper.size)
    }

    @ParameterizedTest
    @EnumSource(names = ["REVURDERING", "SØKNAD"], mode = EnumSource.Mode.INCLUDE)
    fun `Mapper oppgavetypeegenskaper riktig`(egenskap: EgenskapForDatabase) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper =
            setOf(
                egenskap,
                EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
                EgenskapForDatabase.EN_ARBEIDSGIVER,
                EgenskapForDatabase.FORSTEGANGSBEHANDLING,
            )
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(
                        oppgaveId = 1L,
                        aktørId = "1234567891011",
                        opprettet = opprettet,
                        opprinneligSøknadsdato = opprinneligSøknadsdato,
                        vedtaksperiodeId = vedtaksperiodeId,
                        personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                        tildelt = SaksbehandlerFraDatabase(
                            SAKSBEHANDLEREPOST,
                            SAKSBEHANDLEROID,
                            SAKSBEHANDLERNAVN,
                            SAKSBEHANDLERIDENT
                        ),
                        påVent = true,
                        egenskaper = egenskaper,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map {
            UUID.fromString(
                idForGruppe(it)
            )
        })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList(), Filtrering())
        val oppgave = oppgaver.oppgaver.single()
        assertEquals(egenskap.oppgavetype(), oppgave.oppgavetype)
    }

    @ParameterizedTest
    @EnumSource(
        names = ["FORLENGELSE", "INFOTRYGDFORLENGELSE", "OVERGANG_FRA_IT", "FORSTEGANGSBEHANDLING"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `Mapper periodetypeegenskaper riktig`(egenskap: EgenskapForDatabase) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper =
            setOf(
                egenskap,
                EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
                EgenskapForDatabase.EN_ARBEIDSGIVER,
                EgenskapForDatabase.SØKNAD
            )
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(
                        oppgaveId = 1L,
                        aktørId = "1234567891011",
                        opprettet = opprettet,
                        opprinneligSøknadsdato = opprinneligSøknadsdato,
                        vedtaksperiodeId = vedtaksperiodeId,
                        personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                        tildelt = SaksbehandlerFraDatabase(
                            SAKSBEHANDLEREPOST,
                            SAKSBEHANDLEROID,
                            SAKSBEHANDLERNAVN,
                            SAKSBEHANDLERIDENT
                        ),
                        påVent = true,
                        egenskaper = egenskaper,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map {
            UUID.fromString(
                idForGruppe(it)
            )
        })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList(), Filtrering())
        val oppgave = oppgaver.oppgaver.single()
        assertEquals(egenskap.periodetype(), oppgave.periodetype)
    }

    @ParameterizedTest
    @EnumSource(
        names = ["UTBETALING_TIL_SYKMELDT", "DELVIS_REFUSJON", "UTBETALING_TIL_ARBEIDSGIVER", "INGEN_UTBETALING"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `Mapper mottakeregenskaper riktig`(egenskap: EgenskapForDatabase) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper =
            setOf(
                egenskap,
                EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                EgenskapForDatabase.EN_ARBEIDSGIVER,
                EgenskapForDatabase.SØKNAD
            )
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(
                        oppgaveId = 1L,
                        aktørId = "1234567891011",
                        opprettet = opprettet,
                        opprinneligSøknadsdato = opprinneligSøknadsdato,
                        vedtaksperiodeId = vedtaksperiodeId,
                        personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                        tildelt = SaksbehandlerFraDatabase(
                            SAKSBEHANDLEREPOST,
                            SAKSBEHANDLEROID,
                            SAKSBEHANDLERNAVN,
                            SAKSBEHANDLERIDENT
                        ),
                        påVent = true,
                        egenskaper = egenskaper,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map {
            UUID.fromString(
                idForGruppe(it)
            )
        })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList(), Filtrering())
        val oppgave = oppgaver.oppgaver.single()
        assertEquals(egenskap.mottaker(), oppgave.mottaker)
    }

    @ParameterizedTest
    @EnumSource(names = ["EN_ARBEIDSGIVER", "FLERE_ARBEIDSGIVERE"], mode = EnumSource.Mode.INCLUDE)
    fun `Mapper inntektskildegenskaper riktig`(egenskap: EgenskapForDatabase) {
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprinneligSøknadsdato = LocalDateTime.now()
        val egenskaper =
            setOf(
                egenskap,
                EgenskapForDatabase.FORSTEGANGSBEHANDLING,
                EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
                EgenskapForDatabase.SØKNAD,
            )
        every { oppgaveDao.finnOppgaverForVisning(ekskluderEgenskaper = any(), saksbehandlerOid = any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(
                        oppgaveId = 1L,
                        aktørId = "1234567891011",
                        opprettet = opprettet,
                        opprinneligSøknadsdato = opprinneligSøknadsdato,
                        vedtaksperiodeId = vedtaksperiodeId,
                        personnavnFraDatabase = PersonnavnFraDatabase("fornavn", "mellomnavn", "etternavn"),
                        tildelt = SaksbehandlerFraDatabase(
                            SAKSBEHANDLEREPOST,
                            SAKSBEHANDLEROID,
                            SAKSBEHANDLERNAVN,
                            SAKSBEHANDLERIDENT
                        ),
                        påVent = true,
                        egenskaper = egenskaper,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi(tilganger = EnumSet.allOf(Gruppe::class.java).map {
            UUID.fromString(
                idForGruppe(it)
            )
        })
        val oppgaver = mediator.oppgaver(saksbehandler, 0, MAX_VALUE, emptyList(), Filtrering())
        val oppgave = oppgaver.oppgaver.single()
        assertEquals(egenskap.antallArbeidsforhold(), oppgave.antallArbeidsforhold)
    }

    private fun assertAntallOpptegnelser(
        antallOpptegnelser: Int,
        fødselsnummer: String,
    ) = verify(exactly = antallOpptegnelser) {
        opptegnelseRepository.opprettOpptegnelse(
            eq(fødselsnummer),
            any(),
            eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE),
        )
    }

    private fun assertOpptegnelseIkkeOpprettet(fødselsnummer: String) = assertAntallOpptegnelser(0, fødselsnummer)

    private fun behandletOppgaveFraDatabaseForVisning(
        oppgaveId: Long = nextLong(),
        aktørId: String = nextLong(1000000000000, 2000000000000).toString(),
        egenskaper: Set<EgenskapForDatabase> = EGENSKAPER,
        ferdigstiltAv: String? = "saksbehandler",
        personnavnFraDatabase: PersonnavnFraDatabase = PersonnavnFraDatabase("navn", "mellomnavn", "etternavn"),
        ferdigstiltTidspunkt: LocalDateTime = LocalDateTime.now(),
        filtrertAntall: Int = 1,
    ) = BehandletOppgaveFraDatabaseForVisning(
        id = oppgaveId,
        aktørId = aktørId,
        egenskaper = egenskaper,
        ferdigstiltAv = ferdigstiltAv,
        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
        navn = personnavnFraDatabase,
        filtrertAntall = filtrertAntall,
    )

    private fun oppgaveFraDatabaseForVisning(
        oppgaveId: Long = nextLong(),
        egenskaper: Set<EgenskapForDatabase> = EGENSKAPER,
        aktørId: String = nextLong(1000000000000, 2000000000000).toString(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        opprinneligSøknadsdato: LocalDateTime = LocalDateTime.now(),
        tidsfrist: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        personnavnFraDatabase: PersonnavnFraDatabase = PersonnavnFraDatabase("navn", "mellomnavn", "etternavn"),
        tildelt: SaksbehandlerFraDatabase? = null,
        påVent: Boolean = false,
        filtrertAntall: Int = 1,
    ) = OppgaveFraDatabaseForVisning(
        id = oppgaveId,
        aktørId = aktørId,
        vedtaksperiodeId = vedtaksperiodeId,
        navn = personnavnFraDatabase,
        egenskaper = egenskaper,
        tildelt = tildelt,
        påVent = påVent,
        opprettet = opprettet,
        opprinneligSøknadsdato = opprinneligSøknadsdato,
        tidsfrist = tidsfrist,
        filtrertAntall = filtrertAntall,
        paVentInfo = null,
    )

    private fun oppgaveFraDatabase(
        oppgaveId: Long = OPPGAVE_ID,
        tildelt: Boolean = false,
    ) = OppgaveFraDatabase(
        id = oppgaveId,
        egenskaper = listOf(EgenskapForDatabase.SØKNAD),
        status = "AvventerSaksbehandler",
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        behandlingId = BEHANDLING_ID,
        utbetalingId = UTBETALING_ID,
        godkjenningsbehovId = HENDELSE_ID,
        kanAvvises = true,
        ferdigstiltAvIdent = null,
        ferdigstiltAvOid = null,
        tildelt =
            if (tildelt) {
                SaksbehandlerFraDatabase(
                    SAKSBEHANDLEREPOST,
                    SAKSBEHANDLEROID,
                    SAKSBEHANDLERNAVN,
                    SAKSBEHANDLERIDENT,
                )
            } else {
                null
            },
    )

    private fun EgenskapForDatabase.periodetype(): Periodetype =
        when (this) {
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
            EgenskapForDatabase.FORLENGELSE -> Periodetype.FORLENGELSE
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> Periodetype.OVERGANG_FRA_IT
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }

    private fun EgenskapForDatabase.oppgavetype(): Oppgavetype =
        when (this) {
            EgenskapForDatabase.SØKNAD -> Oppgavetype.SOKNAD
            EgenskapForDatabase.REVURDERING -> Oppgavetype.REVURDERING
            EgenskapForDatabase.STIKKPRØVE -> Oppgavetype.STIKKPROVE
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }

    private fun EgenskapForDatabase.mottaker(): Mottaker =
        when (this) {
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> Mottaker.SYKMELDT
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> Mottaker.ARBEIDSGIVER
            EgenskapForDatabase.DELVIS_REFUSJON -> Mottaker.BEGGE
            EgenskapForDatabase.INGEN_UTBETALING -> Mottaker.INGEN
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }

    private fun EgenskapForDatabase.antallArbeidsforhold(): AntallArbeidsforhold =
        when (this) {
            EgenskapForDatabase.EN_ARBEIDSGIVER -> AntallArbeidsforhold.ET_ARBEIDSFORHOLD
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
}
