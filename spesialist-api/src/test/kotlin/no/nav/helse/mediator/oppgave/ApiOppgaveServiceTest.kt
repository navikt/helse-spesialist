package no.nav.helse.mediator.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiKategori
import no.nav.helse.spesialist.api.graphql.schema.ApiMottaker
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgrupper
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlernavn
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.EnumSet
import java.util.UUID
import kotlin.random.Random

internal class ApiOppgaveServiceTest {
    private val tilgangsgrupper: Tilgangsgrupper = randomTilgangsgrupper()
    private val SAKSBEHANDLERIDENT = lagSaksbehandlerident()
    private val SAKSBEHANDLEROID = UUID.randomUUID()
    private val SAKSBEHANDLERNAVN = lagSaksbehandlernavn()
    private val SAKSBEHANDLEREPOST = lagEpostadresseFraFulltNavn(SAKSBEHANDLERNAVN)
    private val EGENSKAPER =
        setOf(
            EgenskapForDatabase.SØKNAD,
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT,
            EgenskapForDatabase.EN_ARBEIDSGIVER,
            EgenskapForDatabase.FORSTEGANGSBEHANDLING,
        )

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val oppgaveRepository = mockk<OppgaveRepository>(relaxed = true)

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

        override fun publiser(fødselsnummer: String, event: KommandokjedeEndretEvent, hendelseNavn: String) =
            error("Not implemented for test")
    }

    private val apiOppgaveService =
        ApiOppgaveService(
            oppgaveDao = oppgaveDao,
            oppgaveService = OppgaveService(
                oppgaveDao = oppgaveDao,
                reservasjonDao = reservasjonDao,
                meldingPubliserer = meldingPubliserer,
                tilgangsgrupper = tilgangsgrupper,
                oppgaveRepository = oppgaveRepository,
                tilgangsgruppehenter = { emptySet() }
            )
        )

    private fun saksbehandlerFraApi(grupper: Set<Tilgangsgruppe> = emptySet()) =
        SaksbehandlerFraApi(SAKSBEHANDLEROID, SAKSBEHANDLEREPOST, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT, tilgangsgrupper.uuiderFor(grupper).toList(), grupper)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, tildelingDao, opptegnelseDao)
    }

    @Test
    fun `Hent oppgaver til visning`() {
        every { oppgaveDao.finnOppgaverForVisning(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(filtrertAntall = 2),
                    oppgaveFraDatabaseForVisning(filtrertAntall = 2),
                )
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandlerFraApi(), 0,
            Int.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent tildelte oppgaver for en gitt saksbehandler`() {
        every { oppgaveDao.finnTildelteOppgaver(any(), any()) } returns
                listOf(
                    oppgaveFraDatabaseForVisning(filtrertAntall = 2),
                    oppgaveFraDatabaseForVisning(filtrertAntall = 2),
                )
        val oppgaver = apiOppgaveService.tildelteOppgaver(
            saksbehandlerFraApi(),
            Saksbehandler(
                id = SaksbehandlerOid(UUID.randomUUID()),
                navn = "Navn Navnesen",
                ident = "L815493",
                epost = "navn@navnesen.no"
            ),
            0,
            Int.MAX_VALUE,
        )
        assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent antall mine saker og mine saker på vent til visning`() {
        every { oppgaveDao.finnAntallOppgaver(any()) } returns AntallOppgaverFraDatabase(
            antallMineSaker = 2,
            antallMineSakerPåVent = 1
        )
        val antallOppgaver = apiOppgaveService.antallOppgaver(saksbehandlerFraApi())
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
        val oppgaver = apiOppgaveService.behandledeOppgaver(
            saksbehandlerFraApi(),
            0,
            Int.MAX_VALUE,
            LocalDate.now(),
            LocalDate.now()
        )
        assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent kun oppgaver til visning som saksbehandler har tilgang til`() {
        apiOppgaveService.oppgaver(saksbehandlerFraApi(), 0, Int.MAX_VALUE, emptyList(), ApiFiltrering())
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = setOf(
                    Egenskap.FORTROLIG_ADRESSE,
                    Egenskap.STRENGT_FORTROLIG_ADRESSE,
                    Egenskap.EGEN_ANSATT,
                    Egenskap.BESLUTTER,
                    Egenskap.STIKKPRØVE
                ).map { it.name },
                saksbehandlerOid = SAKSBEHANDLEROID,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
        }
    }

    @Test
    fun `Ekskluderer alle ukategoriserte egenskaper hvis ingenUkategoriserteEgenskaper i Filtrering er satt til true`() {
        apiOppgaveService.oppgaver(
            saksbehandlerFraApi = saksbehandlerFraApi(),
            offset = 0,
            limit = Int.MAX_VALUE,
            sortering = emptyList(),
            filtrering = ApiFiltrering(ingenUkategoriserteEgenskaper = true)
        )
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper =
                    setOf(
                        Egenskap.FORTROLIG_ADRESSE,
                        Egenskap.STRENGT_FORTROLIG_ADRESSE,
                        Egenskap.EGEN_ANSATT,
                        Egenskap.BESLUTTER,
                        Egenskap.STIKKPRØVE
                    ).map { it.name } +
                            Egenskap.entries
                                .filter { it.kategori == Egenskap.Kategori.Ukategorisert }
                                .map { it.name },
                SAKSBEHANDLEROID,
                0,
                Int.MAX_VALUE,
            )
        }
    }

    @Test
    fun `Ekskluderer alle ekskluderteEgenskaper`() {
        apiOppgaveService.oppgaver(
            saksbehandlerFraApi(),
            0,
            Int.MAX_VALUE,
            emptyList(),
            ApiFiltrering(
                ekskluderteEgenskaper =
                    listOf(
                        ApiOppgaveegenskap(
                            egenskap = ApiEgenskap.PA_VENT,
                            kategori = ApiKategori.Status,
                        ),
                    ),
            ),
        )
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = setOf(
                    Egenskap.FORTROLIG_ADRESSE,
                    Egenskap.STRENGT_FORTROLIG_ADRESSE,
                    Egenskap.EGEN_ANSATT,
                    Egenskap.BESLUTTER,
                    Egenskap.STIKKPRØVE,
                    Egenskap.PÅ_VENT
                ).map { it.name },
                SAKSBEHANDLEROID,
                0,
                Int.MAX_VALUE,
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
                        ferdigstiltAv = "EN-SAKSBEHANDLER-IDENT",
                        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
                    ),
                )
        val saksbehandler = saksbehandlerFraApi()
        val oppgaver =
            apiOppgaveService.behandledeOppgaver(saksbehandler, 0, Int.MAX_VALUE, LocalDate.now(), LocalDate.now())
        assertEquals(1, oppgaver.oppgaver.size)
        val oppgave = oppgaver.oppgaver.single()
        assertEquals("1", oppgave.id)
        assertEquals("1234567891011", oppgave.aktorId)
        assertEquals("fornavn", oppgave.personnavn.fornavn)
        assertEquals("mellomnavn", oppgave.personnavn.mellomnavn)
        assertEquals("etternavn", oppgave.personnavn.etternavn)
        assertEquals("EN-SAKSBEHANDLER-IDENT", oppgave.ferdigstiltAv)
        assertNull(oppgave.beslutter)
        assertEquals("EN-SAKSBEHANDLER-IDENT", oppgave.saksbehandler)
        assertEquals(ferdigstiltTidspunkt, oppgave.ferdigstiltTidspunkt)
        assertEquals(ApiOppgavetype.SOKNAD, oppgave.oppgavetype)
        assertEquals(ApiPeriodetype.FORSTEGANGSBEHANDLING, oppgave.periodetype)
        assertEquals(ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD, oppgave.antallArbeidsforhold)
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
        val saksbehandler = saksbehandlerFraApi(grupper = EnumSet.allOf(Tilgangsgruppe::class.java))
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
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
        val saksbehandler = saksbehandlerFraApi(grupper = EnumSet.allOf(Tilgangsgruppe::class.java))
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
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
        val saksbehandler = saksbehandlerFraApi(grupper = EnumSet.allOf(Tilgangsgruppe::class.java))
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
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
        val saksbehandler = saksbehandlerFraApi(grupper = EnumSet.allOf(Tilgangsgruppe::class.java))
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
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
        val saksbehandler = saksbehandlerFraApi(grupper = EnumSet.allOf(Tilgangsgruppe::class.java))
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        val oppgave = oppgaver.oppgaver.single()
        assertEquals(egenskap.antallArbeidsforhold(), oppgave.antallArbeidsforhold)
    }

    private fun behandletOppgaveFraDatabaseForVisning(
        oppgaveId: Long = Random.Default.nextLong(),
        aktørId: String = Random.Default.nextLong(1000000000000, 2000000000000).toString(),
        egenskaper: Set<EgenskapForDatabase> = EGENSKAPER,
        ferdigstiltAv: String? = "EN-SAKSBEHANDLER-IDENT",
        beslutter: String? = null,
        saksbehandler: String? = null,
        personnavnFraDatabase: PersonnavnFraDatabase = PersonnavnFraDatabase("navn", "mellomnavn", "etternavn"),
        ferdigstiltTidspunkt: LocalDateTime = LocalDateTime.now(),
        filtrertAntall: Int = 1,
    ) = BehandletOppgaveFraDatabaseForVisning(
        id = oppgaveId,
        aktørId = aktørId,
        egenskaper = egenskaper,
        ferdigstiltAv = ferdigstiltAv,
        saksbehandler = saksbehandler ?: ferdigstiltAv,
        beslutter = beslutter,
        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
        navn = personnavnFraDatabase,
        filtrertAntall = filtrertAntall,
    )

    private fun oppgaveFraDatabaseForVisning(
        oppgaveId: Long = Random.Default.nextLong(),
        egenskaper: Set<EgenskapForDatabase> = EGENSKAPER,
        aktørId: String = Random.Default.nextLong(1000000000000, 2000000000000).toString(),
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

    private fun EgenskapForDatabase.periodetype(): ApiPeriodetype =
        when (this) {
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> ApiPeriodetype.FORSTEGANGSBEHANDLING
            EgenskapForDatabase.FORLENGELSE -> ApiPeriodetype.FORLENGELSE
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> ApiPeriodetype.INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> ApiPeriodetype.OVERGANG_FRA_IT
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }

    private fun EgenskapForDatabase.oppgavetype(): ApiOppgavetype =
        when (this) {
            EgenskapForDatabase.SØKNAD -> ApiOppgavetype.SOKNAD
            EgenskapForDatabase.REVURDERING -> ApiOppgavetype.REVURDERING
            EgenskapForDatabase.STIKKPRØVE -> ApiOppgavetype.STIKKPROVE
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }

    private fun EgenskapForDatabase.mottaker(): ApiMottaker =
        when (this) {
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> ApiMottaker.SYKMELDT
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> ApiMottaker.ARBEIDSGIVER
            EgenskapForDatabase.DELVIS_REFUSJON -> ApiMottaker.BEGGE
            EgenskapForDatabase.INGEN_UTBETALING -> ApiMottaker.INGEN
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }

    private fun EgenskapForDatabase.antallArbeidsforhold(): ApiAntallArbeidsforhold =
        when (this) {
            EgenskapForDatabase.EN_ARBEIDSGIVER -> ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> ApiAntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
}
