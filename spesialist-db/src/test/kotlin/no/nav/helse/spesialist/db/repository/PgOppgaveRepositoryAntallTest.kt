package no.nav.helse.spesialist.db.repository

import no.nav.helse.mediator.oppgave.OppgaveRepository.AntallOppgaverProjeksjon
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PgOppgaveRepositoryAntallTest : AbstractDBIntegrationTest() {
    private val repository = PgOppgaveRepository(session)

    @Test
    fun `teller ingen saker når saksbehandler ikke har tildelinger`() {
        nyOppgaveForNyPerson()

        val saksbehandlerWrapper = nyLegacySaksbehandler()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller mine saker riktig`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 2, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller mine saker på vent riktig`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper).leggPåVentOgLagre(saksbehandlerWrapper)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper).leggPåVentOgLagre(saksbehandlerWrapper)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 2), antall)
    }

    @Test
    fun `skiller mellom vanlige saker og saker på vent`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper).leggPåVentOgLagre(saksbehandlerWrapper)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 2, antallMineSakerPåVent = 1), antall)
    }

    @Test
    fun `teller ikke oppgaver tildelt til andre saksbehandlere`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()
        val annenSaksbehandler = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler).leggPåVentOgLagre(annenSaksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller kun egne saker blant oppgaver tildelt til flere saksbehandlere`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()
        val annenSaksbehandler = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper).leggPåVentOgLagre(saksbehandlerWrapper)
        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler)
        nyOppgaveForNyPerson().tildelOgLagre(annenSaksbehandler).leggPåVentOgLagre(annenSaksbehandler)

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 1, antallMineSakerPåVent = 1), antall)
    }

    @Test
    fun `teller ikke ferdigstilte oppgaver`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper).avventSystemOgLagre(saksbehandlerWrapper).ferdigstillOgLagre()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `teller ikke invaliderte oppgaver`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()

        nyOppgaveForNyPerson().tildelOgLagre(saksbehandlerWrapper).invaliderOgLagre()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }

    @Test
    fun `returnerer null-verdier når ingen oppgaver finnes i databasen`() {
        val saksbehandlerWrapper = nyLegacySaksbehandler()

        val antall = repository.finnAntallOppgaverProjeksjon(saksbehandlerWrapper.saksbehandler.id)

        assertEquals(AntallOppgaverProjeksjon(antallMineSaker = 0, antallMineSakerPåVent = 0), antall)
    }
}
