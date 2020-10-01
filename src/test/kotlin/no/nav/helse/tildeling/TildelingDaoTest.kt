package no.nav.helse.tildeling

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
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
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertEquals(SAKSBEHANDLEREPOST, saksbehandlerepost)
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
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertNull(saksbehandlerepost)
    }

    @Test
    fun `henter den siste saksbehandlereposten for tildeling med fødselsnummer`() {
        val nyHendelseId = UUID.randomUUID()
        val nySaksbehandlerepost = "ny.saksbehandler@nav.no"
        nyPerson()
        tildelTilSaksbehandler()
        opprettVedtaksperiode(vedtaksperiodeId = UUID.randomUUID())
        opprettOppgave(hendelseId = nyHendelseId, vedtakId = vedtakId)
        tildelTilSaksbehandler(
            oppgaveId = oppgaveId,
            oid = UUID.randomUUID(),
            navn = "Ny Saksbehandler",
            epost = nySaksbehandlerepost
        )
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertEquals(nySaksbehandlerepost, saksbehandlerepost)
    }

    @Test
    fun `utgått tildeling blir ikke tatt med i snapshot`() {
        val saksbehandlerOid = UUID.randomUUID()
        val oppgavereferanse = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandlerEpost = "${UUID.randomUUID()}@nav.no"
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId)
        opprettOppgave()
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerOid, "Sara Saksbehandler", saksbehandlerEpost)
        tildelingDao.opprettTildeling(oppgaveId, saksbehandlerOid, LocalDateTime.now().minusDays(1))
        assertNull(tildelingDao.finnSaksbehandlerEpost(oppgaveId))
        assertNull(tildelingDao.finnSaksbehandlerNavn(oppgaveId))
        assertNull(tildelingDao.tildelingForPerson(FNR))
        assertTrue(oppgaveDao.finnOppgaver().none { it.saksbehandlerepost == saksbehandlerEpost })
    }

    private fun tildelTilSaksbehandler(
        oppgaveId: Long = this.oppgaveId,
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = "Sara Saksbehandler",
        epost: String = SAKSBEHANDLEREPOST
    ) = sessionOf(dataSource).use {
        it.persisterSaksbehandler(oid, navn, epost)
        it.tildelOppgave(oppgaveId, oid)
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
}
