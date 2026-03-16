package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.EgenskapForDatabase.PÅ_VENT
import no.nav.helse.db.EgenskapForDatabase.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgOppgaveDaoTest : AbstractDBIntegrationTest() {
    private val legacySaksbehandler = nyLegacySaksbehandler()

    @Test
    fun `Finn neste ledige id`() {
        assertDoesNotThrow {
            oppgaveDao.reserverNesteId()
            oppgaveDao.reserverNesteId()
        }
    }

    @Test
    fun `finn SpleisBehandlingId`() {
        val oppgave = nyOppgaveForNyPerson()
        assertEquals(oppgave.behandlingId, oppgaveDao.finnSpleisBehandlingId(oppgave.id))
    }

    @Test
    fun `finn behandlingId`() {
        val oppgave = nyOppgaveForNyPerson()
        assertDoesNotThrow {
            oppgaveDao.finnBehandlingId(oppgave.id)
        }
    }

    @Test
    fun `finner oppgaveId ved hjelp av fødselsnummer`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)

        assertEquals(oppgave.id, oppgaveDao.finnOppgaveId(fødselsnummer))
    }

    @Test
    fun `finner oppgaveId for ikke-avsluttet oppgave ved hjelp av vedtaksperiodeId`() {
        val oppgave = nyOppgaveForNyPerson()

        assertEquals(oppgave.id, oppgaveDao.finnIdForAktivOppgave(oppgave.vedtaksperiodeId))

        oppgave.invaliderOgLagre()
        assertNull(oppgaveDao.finnIdForAktivOppgave(oppgave.vedtaksperiodeId))
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        val oppgave = nyOppgaveForNyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgave.id)
        assertEquals(oppgave.vedtaksperiodeId, actual)
    }

    @Test
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        val oppgave = nyOppgaveForNyPerson()

        assertFalse(oppgaveDao.harFerdigstiltOppgave(oppgave.vedtaksperiodeId))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        val oppgave =
            nyOppgaveForNyPerson()
                .avventSystemOgLagre(legacySaksbehandler)
                .ferdigstillOgLagre()

        assertTrue(oppgaveDao.harFerdigstiltOppgave(oppgave.vedtaksperiodeId))
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
        val funnetFødselsnummer = oppgaveDao.finnFødselsnummer(oppgave.id)
        assertEquals(funnetFødselsnummer, fødselsnummer)
    }

    @Test
    fun `Finner vedtaksperiodeId med oppgaveId`() {
        val oppgave = nyOppgaveForNyPerson()

        assertEquals(oppgave.vedtaksperiodeId, oppgaveDao.finnVedtaksperiodeId(oppgave.id))
    }

    @Test
    fun `Finner egenskaper for gitt vedtaksperiode med gitt utbetalingId`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.PÅ_VENT))

        val egenskaperOppgaveId1 = oppgaveDao.finnEgenskaper(oppgave1.vedtaksperiodeId, oppgave1.utbetalingId)
        val egenskaperOppgaveId2 = oppgaveDao.finnEgenskaper(oppgave2.vedtaksperiodeId, oppgave2.utbetalingId)
        assertEquals(setOf(SØKNAD), egenskaperOppgaveId1)
        assertEquals(setOf(SØKNAD, PÅ_VENT), egenskaperOppgaveId2)
    }
}
