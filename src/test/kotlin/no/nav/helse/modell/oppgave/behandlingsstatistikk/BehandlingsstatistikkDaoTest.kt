package no.nav.helse.modell.oppgave.behandlingsstatistikk

import DatabaseIntegrationTest
import no.nav.helse.modell.Oppgavestatus
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype.FORLENGELSE
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class BehandlingsstatistikkDaoTest : DatabaseIntegrationTest() {

    private val NOW = LocalDate.now()

    @Test
    fun happyCase() {
        nyPerson()
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(0, dto.antallGodkjenteOppgaver)
        assertEquals(0, dto.antallTildelteOppgaver)
        assertEquals(0, dto.antallAnnulleringer)
        assertEquals(1, dto.oppgaverTilGodkjenning.totalt)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype.size)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype[FØRSTEGANGSBEHANDLING])
    }

    @Test
    fun antallTildelteOppgaver() {
        nyPerson()
        opprettSaksbehandler()
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLER_OID)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.antallTildelteOppgaver)
    }

    @Test
    fun antallGodkjenteOppgaver() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, Oppgavestatus.Ferdigstilt)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.antallGodkjenteOppgaver)
    }

    @Test
    fun antallAnnulleringer() {
        opprettSaksbehandler()
        utbetalingDao.nyAnnullering(LocalDateTime.now(), SAKSBEHANDLER_OID)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(1, dto.antallAnnulleringer)
    }

    @Test
    fun `flere periodetyper`() {
        nyPerson()
        nyVedtaksperiode(FORLENGELSE)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(NOW)
        assertEquals(2, dto.oppgaverTilGodkjenning.totalt)
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype[FØRSTEGANGSBEHANDLING])
        assertEquals(1, dto.oppgaverTilGodkjenning.perPeriodetype[FORLENGELSE])
    }

    @Test
    fun `tar ikke med innslag som er eldre enn dato som sendes inn`() {
        nyPerson()
        val fremtidigDato = NOW.plusDays(1)
        val dto = behandlingsstatistikkDao.oppgavestatistikk(fremtidigDato)
        assertEquals(0, dto.antallGodkjenteOppgaver)
        assertEquals(0, dto.antallTildelteOppgaver)
        assertEquals(0, dto.antallAnnulleringer)
        assertEquals(0, dto.oppgaverTilGodkjenning.totalt)
        assertEquals(0, dto.oppgaverTilGodkjenning.perPeriodetype.size)
    }
}
