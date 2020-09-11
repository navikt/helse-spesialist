package no.nav.helse.tildeling

import AbstractEndToEndTest
import kotliquery.sessionOf
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class TildelingDaoTest : AbstractEndToEndTest() {

    @Test
    fun `henter saksbehandlerepost for tildeling med fødselsnummer`() {
        nyPerson()
        tildelTilSaksbehandler()
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertEquals(SAKSBEHANDLEREPOST, saksbehandlerepost)
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
}
