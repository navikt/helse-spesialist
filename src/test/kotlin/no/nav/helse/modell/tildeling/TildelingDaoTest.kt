package no.nav.helse.modell.tildeling

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.Oppgavestatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TildelingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `oppretter tildeling`() {
        nyPerson()
        val saksbehandleroid = UUID.randomUUID()
        saksbehandlerDao.opprettSaksbehandler(saksbehandleroid, "Navn Navnesen", "navn@navnesen.no")
        tildelingDao.opprettTildeling(oppgaveId, saksbehandleroid)
        assertTildeling(oppgaveId, saksbehandleroid)
    }

    @Test
    fun `henter saksbehandlerepost for tildeling med fødselsnummer`() {
        nyPerson()
        tildelTilSaksbehandler()
        val tildeling = tildelingDao.tildelingForPerson(FNR)
        assertEquals(SAKSBEHANDLEREPOST, tildeling?.epost)
    }

    @Test
    fun `slett tildeling`() {
        nyPerson()
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLER_OID)
        assertTildeling(oppgaveId, SAKSBEHANDLER_OID)
        tildelingDao.slettTildeling(oppgaveId)
        assertTildeling(oppgaveId, null)
    }

    @Test
    fun `henter bare tildelinger som har en aktiv oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        oppgaveDao.updateOppgave(oppgaveId, Oppgavestatus.Ferdigstilt, SAKSBEHANDLEREPOST, SAKSBEHANDLER_OID)
        tildelTilSaksbehandler()
        val saksbehandlerepost = tildelingDao.tildelingForPerson(FNR)
        assertNull(saksbehandlerepost)
    }

    @Test
    fun `henter den siste saksbehandlereposten for tildeling med fødselsnummer`() {
        val nySaksbehandlerepost = "ny.saksbehandler@nav.no"
        nyPerson()
        tildelTilSaksbehandler()
        opprettVedtaksperiode(vedtaksperiodeId = UUID.randomUUID())
        opprettOppgave(vedtakId = vedtakId)
        tildelTilSaksbehandler(
            oppgaveId = oppgaveId,
            oid = UUID.randomUUID(),
            navn = "Ny Saksbehandler",
            epost = nySaksbehandlerepost
        )
        val tildeling = tildelingDao.tildelingForPerson(FNR)
        assertEquals(nySaksbehandlerepost, tildeling?.epost)
    }

    @Test
    fun `utgått tildeling blir ikke tatt med i snapshot`() {
        val saksbehandlerOid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandlerEpost = "${UUID.randomUUID()}@nav.no"
        opprettPerson()
        opprettArbeidsgiver()
        val vedtakId = opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId)
        opprettOppgave(vedtakId = vedtakId)
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerOid, "Sara Saksbehandler", saksbehandlerEpost)
        tildelingDao.opprettTildeling(oppgaveId, saksbehandlerOid, LocalDateTime.now().minusDays(1))
        assertNull(tildelingDao.tildelingForOppgave(oppgaveId))
        assertNull(tildelingDao.tildelingForPerson(FNR))
        oppgaveDao.finnOppgaver(false).also { oppgaver ->
            assertTrue(oppgaver.isNotEmpty())
            assertTrue(oppgaver.none { it.tildeling?.epost == saksbehandlerEpost })
        }
    }

    @Test
    fun `legger opppgave på vent`() {
        nyPerson()
        tildelTilSaksbehandler()
        tildelingDao.leggOppgavePåVent(oppgaveId)
        assertTrue(assertOppgavePåVent(oppgaveId))
    }

    @Test
    fun `fjern på vent`() {
        nyPerson()
        tildelTilSaksbehandler()
        tildelingDao.leggOppgavePåVent(oppgaveId)
        tildelingDao.fjernPåVent(oppgaveId)
        assertFalse(assertOppgavePåVent(oppgaveId))
    }

    @Test
    fun `finn tildeling for oppgave`() {
        nyPerson()
        tildelTilSaksbehandler()
        val tildeling = tildelingDao.tildelingForOppgave(this.oppgaveId)
        assertEquals(SAKSBEHANDLER_OID, tildeling?.oid)
        assertEquals(SAKSBEHANDLEREPOST, tildeling?.epost)
        assertEquals(SAKSBEHANDLER_NAVN, tildeling?.navn)
        assertEquals(false, tildeling?.påVent)
    }

    private fun tildelTilSaksbehandler(
        oppgaveId: Long = this.oppgaveId,
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER_NAVN,
        epost: String = SAKSBEHANDLEREPOST
    ) {
        saksbehandlerDao.opprettSaksbehandler(oid, navn, epost)
        tildelingDao.tildelOppgave(oppgaveId, oid)
    }

    private fun assertTildeling(oppgaveId: Long, saksbehandleroid: UUID?) {
        val result = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT saksbehandler_ref FROM tildeling WHERE oppgave_id_ref = ?", oppgaveId)
                    .map { UUID.fromString(it.string("saksbehandler_ref")) }.asSingle
            )
        }
        assertEquals(saksbehandleroid, result)
    }

    private fun assertOppgavePåVent(oppgaveId: Long) : Boolean {
        return requireNotNull(using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT på_vent FROM tildeling WHERE oppgave_id_ref = ?", oppgaveId)
                    .map { it.boolean("på_vent") }.asSingle
            )
        })
    }
}
