package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PgSaksbehandlerDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `henter saksbehandler for ident`() {
        val ident = "L123456"
        opprettSaksbehandler(ident = ident)

        val resultat = saksbehandlerDao.hent(ident)
        assertNotNull(resultat)
        assertEquals(ident, resultat?.ident)
    }

    @Test
    fun `ukjent saksbehandler-ident retunerer null`() {
        opprettSaksbehandler(ident = "L112233")
        val resultat = saksbehandlerDao.hent("E654321")
        assertNull(resultat)
    }
}
