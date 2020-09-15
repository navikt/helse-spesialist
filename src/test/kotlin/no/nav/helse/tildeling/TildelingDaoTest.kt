package no.nav.helse.tildeling

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TildelingDaoTest : AbstractEndToEndTest() {

    @Test
    fun `oppretter tildeling`() {
        val oppgavereferanse = UUID.randomUUID()
        val saksbehandleroid = UUID.randomUUID()
        saksbehandlerDao.opprettSaksbehandler(saksbehandleroid, "Navn Navnesen", "navn@navnesen.no")
        tildelingDao.tildelOppgave(oppgavereferanse, saksbehandleroid)
        assertTildeling(oppgavereferanse, saksbehandleroid)
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
    fun `henter bare tildelinger som har en aktiv oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(oppgavestatus = Oppgavestatus.Ferdigstilt)
        tildelTilSaksbehandler()
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertNull(saksbehandlerepost)
    }

    @Test
    fun `henter den siste saksbehandlereposten for tildeling med fødselsnummer`() {
        val nyOppgavereferanse = UUID.randomUUID()
        val nySaksbehandlerepost = "ny.saksbehandler@nav.no"
        nyPerson()
        tildelTilSaksbehandler()
        opprettVedtaksperiode(vedtaksperiodeId = UUID.randomUUID())
        opprettOppgave(oppgavereferanse = nyOppgavereferanse)
        tildelTilSaksbehandler(
            hendelseId = nyOppgavereferanse,
            oid = UUID.randomUUID(),
            navn = "Ny Saksbehandler",
            epost = nySaksbehandlerepost
        )
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertEquals(nySaksbehandlerepost, saksbehandlerepost)
    }

    private fun tildelTilSaksbehandler(
        hendelseId: UUID = HENDELSE_ID,
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = "Sara Saksbehandler",
        epost: String = SAKSBEHANDLEREPOST
    ) = sessionOf(dataSource).use {
        it.persisterSaksbehandler(oid, navn, epost)
        it.tildelOppgave(hendelseId, oid)
    }

    private fun assertTildeling(oppgavereferanse: UUID, saksbehandleroid: UUID) {
        val result = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT saksbehandler_ref FROM tildeling WHERE oppgave_ref = ?", oppgavereferanse)
                    .map { UUID.fromString(it.string("saksbehandler_ref")) }.asSingle
            )
        }
        assertEquals(saksbehandleroid, result)
    }
}
