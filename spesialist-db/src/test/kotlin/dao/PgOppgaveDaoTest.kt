package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.EgenskapForDatabase.PÅ_VENT
import no.nav.helse.db.EgenskapForDatabase.SØKNAD
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgOppgaveDaoTest : AbstractDBIntegrationTest() {
    private val saksbehandler = lagSaksbehandler()

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
        assertEquals(oppgave.behandlingId.value, oppgaveDao.finnSpleisBehandlingId(oppgave.id.value))
    }

    @Test
    fun `finn behandlingId`() {
        val oppgave = nyOppgaveForNyPerson()
        assertDoesNotThrow {
            oppgaveDao.finnBehandlingId(oppgave.id.value)
        }
    }

    @Test
    fun `finner oppgaveId ved hjelp av fødselsnummer`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)

        assertEquals(oppgave.id.value, oppgaveDao.finnOppgaveId(fødselsnummer))
    }

    @Test
    fun `finner oppgaveId for ikke-avsluttet oppgave ved hjelp av vedtaksperiodeId`() {
        val oppgave = nyOppgaveForNyPerson()

        assertEquals(oppgave.id.value, oppgaveDao.finnIdForAktivOppgave(oppgave.vedtaksperiodeId.value))

        oppgave.invaliderOgLagre()
        assertNull(oppgaveDao.finnIdForAktivOppgave(oppgave.vedtaksperiodeId.value))
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        val oppgave = nyOppgaveForNyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgave.id.value)
        assertEquals(oppgave.vedtaksperiodeId.value, actual)
    }

    @Test
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        val oppgave = nyOppgaveForNyPerson()

        assertFalse(oppgaveDao.harFerdigstiltOppgave(oppgave.vedtaksperiodeId.value))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        val oppgave =
            nyOppgaveForNyPerson()
                .avventSystemOgLagre(saksbehandler)
                .ferdigstillOgLagre()

        assertTrue(oppgaveDao.harFerdigstiltOppgave(oppgave.vedtaksperiodeId.value))
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
        val funnetFødselsnummer = oppgaveDao.finnFødselsnummer(oppgave.id.value)
        assertEquals(funnetFødselsnummer, fødselsnummer)
    }

    @Test
    fun `Finner vedtaksperiodeId med oppgaveId`() {
        val oppgave = nyOppgaveForNyPerson()

        assertEquals(oppgave.vedtaksperiodeId.value, oppgaveDao.finnVedtaksperiodeId(oppgave.id.value))
    }

    @Test
    fun `Finner egenskaper for gitt vedtaksperiode med gitt utbetalingId`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.PÅ_VENT))

        val egenskaperOppgaveId1 = oppgaveDao.finnEgenskaper(oppgave1.vedtaksperiodeId.value, oppgave1.utbetalingId)
        val egenskaperOppgaveId2 = oppgaveDao.finnEgenskaper(oppgave2.vedtaksperiodeId.value, oppgave2.utbetalingId)
        assertEquals(setOf(SØKNAD), egenskaperOppgaveId1)
        assertEquals(setOf(SØKNAD, PÅ_VENT), egenskaperOppgaveId2)
    }
}
