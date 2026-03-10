package no.nav.helse.mediator.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.api.graphql.ApiOppgaveService
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class ApiOppgaveServiceTest {
    private val SAKSBEHANDLER = lagSaksbehandler()
    private val SAKSBEHANDLERIDENT = SAKSBEHANDLER.ident
    private val SAKSBEHANDLEROID = SAKSBEHANDLER.id.value
    private val SAKSBEHANDLERNAVN = SAKSBEHANDLER.navn
    private val SAKSBEHANDLEREPOST = SAKSBEHANDLER.epost

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
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
                oppgaveRepository = oppgaveRepository,
                brukerrollehenter = { Either.Success(emptySet()) },
            )
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
}
