package no.nav.helse.modell.saksbehandler

import DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

internal class SaksbehandlerDaoTest : DatabaseIntegrationTest() {

    private companion object {
        private const val IS_UPDATED = 1
        private const val IS_NOT_UPDATED = 0
    }

    @Test
    fun `oppretter og finner saksbehandler`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
        assertSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
    }

    @Test
    fun `Oppdaterer saksbehandlers navn og epost ved konflikt på oid`() {
        val (nyttNavn, nyEpost) = Pair("Navn Navne Navnesen", "navn.navne.navnesen@nav.no")
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
        assertEquals(IS_UPDATED, saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, nyttNavn, nyEpost))
        assertSaksbehandler(SAKSBEHANDLER_OID, nyttNavn, nyEpost)
    }

    @Test
    fun `Oppdaterer ikke saksbehandlers navn og epost ved konflikt på oid dersom navn og epost er uendret`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST)
        assertEquals(IS_NOT_UPDATED, saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST))
        assertSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST)
    }

    private fun saksbehandler(oid: UUID) = saksbehandlerDao.finnSaksbehandler(oid)

    private fun assertSaksbehandler(oid: UUID, navn: String, epost: String) {
        val saksbehandler = saksbehandler(oid)
        assertNotNull(saksbehandler)
        assertEquals(navn, saksbehandler?.navn)
        assertEquals(epost, saksbehandler?.epost)
    }
}
