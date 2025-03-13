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
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiKategori
import no.nav.helse.spesialist.api.graphql.schema.ApiMottaker
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.api.lagSaksbehandlerident
import no.nav.helse.spesialist.api.lagSaksbehandlernavn
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.junit.jupiter.api.Assertions
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
    private fun idForGruppe(gruppe: Gruppe) = SpeilTilgangsgrupper(testEnv).gruppeId(gruppe).toString()
    private val testEnv = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }

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
            tilgangsgrupper = SpeilTilgangsgrupper(testEnv),
            oppgaveService = OppgaveService(
                oppgaveDao = oppgaveDao,
                reservasjonDao = reservasjonDao,
                meldingPubliserer = meldingPubliserer,
                tilgangskontroll = { _, _ -> false },
                tilgangsgrupper = SpeilTilgangsgrupper(testEnv),
                oppgaveRepository = oppgaveRepository
            )
        )

    private fun saksbehandlerFraApi(tilganger: List<UUID> = emptyList()) =
        SaksbehandlerFraApi(SAKSBEHANDLEROID, SAKSBEHANDLEREPOST, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT, tilganger)

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
            Int.Companion.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        Assertions.assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent antall mine saker og mine saker på vent til visning`() {
        every { oppgaveDao.finnAntallOppgaver(any()) } returns AntallOppgaverFraDatabase(
            antallMineSaker = 2,
            antallMineSakerPåVent = 1
        )
        val antallOppgaver = apiOppgaveService.antallOppgaver(saksbehandlerFraApi())
        Assertions.assertEquals(2, antallOppgaver.antallMineSaker)
        Assertions.assertEquals(1, antallOppgaver.antallMineSakerPaVent)
    }

    @Test
    fun `Hent behandlede oppgaver til visning`() {
        every { oppgaveDao.finnBehandledeOppgaver(any()) } returns
                listOf(
                    behandletOppgaveFraDatabaseForVisning(filtrertAntall = 2),
                    behandletOppgaveFraDatabaseForVisning(filtrertAntall = 2),
                )
        val oppgaver = apiOppgaveService.behandledeOppgaver(saksbehandlerFraApi(), 0, Int.Companion.MAX_VALUE)
        Assertions.assertEquals(2, oppgaver.oppgaver.size)
    }

    @Test
    fun `Hent kun oppgaver til visning som saksbehandler har tilgang til`() {
        apiOppgaveService.oppgaver(saksbehandlerFraApi(), 0, Int.Companion.MAX_VALUE, emptyList(), ApiFiltrering())
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = Egenskap.Companion.alleTilgangsstyrteEgenskaper.map { it.name },
                SAKSBEHANDLEROID,
                0,
                Int.Companion.MAX_VALUE,
            )
        }
    }

    @Test
    fun `Ekskluderer alle ukategoriserte egenskaper hvis ingenUkategoriserteEgenskaper i Filtrering er satt til true`() {
        apiOppgaveService.oppgaver(
            saksbehandlerFraApi = saksbehandlerFraApi(),
            offset = 0,
            limit = Int.Companion.MAX_VALUE,
            sortering = emptyList(),
            filtrering = ApiFiltrering(ingenUkategoriserteEgenskaper = true)
        )
        verify(exactly = 1) {
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper =
                    Egenskap.Companion.alleTilgangsstyrteEgenskaper.map { it.name } + Egenskap.Companion.alleUkategoriserteEgenskaper.map { it.name },
                SAKSBEHANDLEROID,
                0,
                Int.Companion.MAX_VALUE,
            )
        }
    }

    @Test
    fun `Ekskluderer alle ekskluderteEgenskaper`() {
        apiOppgaveService.oppgaver(
            saksbehandlerFraApi(),
            0,
            Int.Companion.MAX_VALUE,
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
                ekskluderEgenskaper = Egenskap.Companion.alleTilgangsstyrteEgenskaper.map { it.name } + Egenskap.PÅ_VENT.name,
                SAKSBEHANDLEROID,
                0,
                Int.Companion.MAX_VALUE,
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
        val oppgaver = apiOppgaveService.behandledeOppgaver(saksbehandler, 0, Int.Companion.MAX_VALUE)
        Assertions.assertEquals(1, oppgaver.oppgaver.size)
        val oppgave = oppgaver.oppgaver.single()
        Assertions.assertEquals("1", oppgave.id)
        Assertions.assertEquals("1234567891011", oppgave.aktorId)
        Assertions.assertEquals("fornavn", oppgave.personnavn.fornavn)
        Assertions.assertEquals("mellomnavn", oppgave.personnavn.mellomnavn)
        Assertions.assertEquals("etternavn", oppgave.personnavn.etternavn)
        Assertions.assertEquals("EN-SAKSBEHANDLER-IDENT", oppgave.ferdigstiltAv)
        assertNull(oppgave.beslutter)
        Assertions.assertEquals("EN-SAKSBEHANDLER-IDENT", oppgave.saksbehandler)
        Assertions.assertEquals(ferdigstiltTidspunkt, oppgave.ferdigstiltTidspunkt)
        Assertions.assertEquals(ApiOppgavetype.SOKNAD, oppgave.oppgavetype)
        Assertions.assertEquals(ApiPeriodetype.FORSTEGANGSBEHANDLING, oppgave.periodetype)
        Assertions.assertEquals(ApiAntallArbeidsforhold.ET_ARBEIDSFORHOLD, oppgave.antallArbeidsforhold)
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
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.Companion.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        Assertions.assertEquals(1, oppgaver.oppgaver.size)
        val oppgave = oppgaver.oppgaver.single()
        Assertions.assertEquals("1", oppgave.id)
        Assertions.assertEquals(opprettet, oppgave.opprettet)
        Assertions.assertEquals(opprinneligSøknadsdato, oppgave.opprinneligSoknadsdato)
        Assertions.assertEquals(vedtaksperiodeId, oppgave.vedtaksperiodeId)
        Assertions.assertEquals("fornavn", oppgave.navn.fornavn)
        Assertions.assertEquals("mellomnavn", oppgave.navn.mellomnavn)
        Assertions.assertEquals("etternavn", oppgave.navn.etternavn)
        Assertions.assertEquals("1234567891011", oppgave.aktorId)
        Assertions.assertEquals(SAKSBEHANDLERNAVN, oppgave.tildeling?.navn)
        Assertions.assertEquals(SAKSBEHANDLEREPOST, oppgave.tildeling?.epost)
        Assertions.assertEquals(SAKSBEHANDLEROID, oppgave.tildeling?.oid)
        Assertions.assertEquals(EGENSKAPER.size, oppgave.egenskaper.size)
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
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.Companion.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        val oppgave = oppgaver.oppgaver.single()
        Assertions.assertEquals(egenskap.oppgavetype(), oppgave.oppgavetype)
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
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.Companion.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        val oppgave = oppgaver.oppgaver.single()
        Assertions.assertEquals(egenskap.periodetype(), oppgave.periodetype)
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
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.Companion.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        val oppgave = oppgaver.oppgaver.single()
        Assertions.assertEquals(egenskap.mottaker(), oppgave.mottaker)
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
        val oppgaver = apiOppgaveService.oppgaver(
            saksbehandler, 0,
            Int.Companion.MAX_VALUE, emptyList(),
            ApiFiltrering()
        )
        val oppgave = oppgaver.oppgaver.single()
        Assertions.assertEquals(egenskap.antallArbeidsforhold(), oppgave.antallArbeidsforhold)
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