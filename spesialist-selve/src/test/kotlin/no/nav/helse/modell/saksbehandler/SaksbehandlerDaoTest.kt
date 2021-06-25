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
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
        assertSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `Oppdaterer saksbehandlers navn, epost og ident ved konflikt på oid`() {
        val (nyttNavn, nyEpost, nyIdent) = Triple(
            "Navn Navne Navnesen",
            "navn.navne.navnesen@nav.no",
            "Z999999"
        )
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
        assertEquals(IS_UPDATED, saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, nyttNavn, nyEpost, nyIdent))
        assertSaksbehandler(SAKSBEHANDLER_OID, nyttNavn, nyEpost, nyIdent)
    }

    @Test
    fun `Oppdaterer ikke saksbehandlers navn, epost og ident ved konflikt på oid dersom navn, epost og ident er uendret`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
        assertEquals(
            IS_NOT_UPDATED,
            saksbehandlerDao.opprettSaksbehandler(
                SAKSBEHANDLER_OID,
                SAKSBEHANDLER_NAVN,
                SAKSBEHANDLEREPOST,
                SAKSBEHANDLER_IDENT
            )
        )
        assertSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
    }

    private fun saksbehandler(oid: UUID) = saksbehandlerDao.finnSaksbehandler(oid)

    private fun assertSaksbehandler(oid: UUID, navn: String, epost: String, ident: String) {
        val saksbehandler = saksbehandler(oid)
        assertNotNull(saksbehandler)
        assertEquals(navn, saksbehandler?.navn)
        assertEquals(epost, saksbehandler?.epost)
        assertEquals(ident, saksbehandler?.ident)
    }
}
