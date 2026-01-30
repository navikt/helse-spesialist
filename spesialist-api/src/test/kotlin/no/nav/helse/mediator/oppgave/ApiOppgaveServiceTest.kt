package no.nav.helse.mediator.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavetype
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

internal class ApiOppgaveServiceTest {
    private val SAKSBEHANDLER = lagSaksbehandler()
    private val SAKSBEHANDLERIDENT = SAKSBEHANDLER.ident
    private val SAKSBEHANDLEROID = SAKSBEHANDLER.id.value
    private val SAKSBEHANDLERNAVN = SAKSBEHANDLER.navn
    private val SAKSBEHANDLEREPOST = SAKSBEHANDLER.epost
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
    private val oppgaveRepository = mockk<OppgaveRepository>(relaxed = true)

    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionFactory = integrationTestFixture.sessionFactory

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
                oppgaveRepository = oppgaveRepository,
                tilgangsgruppehenter = { Either.Success(emptySet<Tilgangsgruppe>() to emptySet()) },
            ),
            sessionFactory = sessionFactory
        )

    private fun saksbehandler() =
        Saksbehandler(
            id = SaksbehandlerOid(SAKSBEHANDLEROID),
            navn = SAKSBEHANDLEREPOST,
            epost = SAKSBEHANDLERNAVN,
            ident = SAKSBEHANDLERIDENT
        )

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, tildelingDao)
    }

    @Test
    fun `Hent antall mine saker og mine saker på vent til visning`() {
        every { oppgaveDao.finnAntallOppgaver(any()) } returns AntallOppgaverFraDatabase(
            antallMineSaker = 2,
            antallMineSakerPåVent = 1
        )
        val antallOppgaver = apiOppgaveService.antallOppgaver(saksbehandler())
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
            saksbehandler(),
            0,
            Int.MAX_VALUE,
            LocalDate.now(),
            LocalDate.now()
        )
        assertEquals(2, oppgaver.oppgaver.size)
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
        val saksbehandler = saksbehandler()
        val oppgaver =
            apiOppgaveService.behandledeOppgaver(saksbehandler, 0, Int.MAX_VALUE, LocalDate.now(), LocalDate.now())
        assertEquals(1, oppgaver.oppgaver.size)
        val oppgave = oppgaver.oppgaver.single()
        assertEquals("1", oppgave.id)
        assertDoesNotThrow { oppgave.personPseudoId }
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

    private fun behandletOppgaveFraDatabaseForVisning(
        oppgaveId: Long = Random.nextLong(),
        aktørId: String = Random.nextLong(1000000000000, 2000000000000).toString(),
        fødselsnummer: String = lagFødselsnummer(),
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
        fødselsnummer = fødselsnummer,
        egenskaper = egenskaper,
        ferdigstiltAv = ferdigstiltAv,
        saksbehandler = saksbehandler ?: ferdigstiltAv,
        beslutter = beslutter,
        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
        navn = personnavnFraDatabase,
        filtrertAntall = filtrertAntall,
    )
}
