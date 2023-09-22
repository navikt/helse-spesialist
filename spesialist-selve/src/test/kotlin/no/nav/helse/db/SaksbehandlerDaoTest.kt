package no.nav.helse.db

import DatabaseIntegrationTest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SaksbehandlerDaoTest: DatabaseIntegrationTest() {
    private val dao = SaksbehandlerDao(dataSource)

    @Test
    fun `lagre saksbehandler`() {
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
        assertSaksbehandler(1, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `oppdater saksbehandler`() {
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(1, SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(0, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLEREPOST, SAKSBEHANDLER_IDENT)
    }

    private fun assertSaksbehandler(forventetAntall: Int, oid: UUID, navn: String, epost: String, ident: String) {
        @Language("PostgreSQL")
        val query = """
           SELECT COUNT(1) FROM saksbehandler WHERE oid = ? AND navn = ? AND epost = ? AND ident = ?
        """

        val funnet = sessionOf(dataSource).use {
            it.run(queryOf(query, oid, navn, epost, ident).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, funnet)
    }
}