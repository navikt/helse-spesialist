package no.nav.helse.spesialist.db.repository

import no.nav.helse.mediator.oppgave.OppgaveRepository.AntallOppgaverProjeksjon
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PgOppgaveRepositoryAntallTest : AbstractDBIntegrationTest() {
    private val repository = PgOppgaveRepository(session)

    @Test
    fun `teller ingen saker når saksbehandler ikke har tildelinger`() {
        nyOppgaveForNyPerson()

        val saksbehandler = lagSaksbehandler()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller mine saker riktig`() {
        val saksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 2, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller mine saker på vent riktig`() {
        val saksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler).leggPåVentOgLagre(saksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler).leggPåVentOgLagre(saksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 2), antall)
    }

    @Test
    fun `skiller mellom vanlige saker og saker på vent`() {
        val saksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler).leggPåVentOgLagre(saksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 2, antallMineSakerPåVent = 1), antall)
    }

    @Test
    fun `teller ikke oppgaver tildelt til andre saksbehandlere`() {
        val saksbehandler = lagSaksbehandler()
        val annenSaksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler).leggPåVentOgLagre(annenSaksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller kun egne saker blant oppgaver tildelt til flere saksbehandlere`() {
        val saksbehandler = lagSaksbehandler()
        val annenSaksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler).leggPåVentOgLagre(saksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler).leggPåVentOgLagre(annenSaksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 1, antallMineSakerPåVent = 1), antall)
    }

    @Test
    fun `teller ikke ferdigstilte oppgaver`() {
        val saksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler).avventSystemOgLagre(saksbehandler).ferdigstillOgLagre()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller ikke invaliderte oppgaver`() {
        val saksbehandler = lagSaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler).invaliderOgLagre()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `returnerer null-verdier når ingen oppgaver finnes i databasen`() {
        val saksbehandler = lagSaksbehandler()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }
}
