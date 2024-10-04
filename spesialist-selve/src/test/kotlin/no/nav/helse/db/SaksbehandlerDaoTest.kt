package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class SaksbehandlerDaoTest: DatabaseIntegrationTest() {
    private val dao = SaksbehandlerDao(dataSource)

    @Test
    fun `lagre saksbehandler`() {
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertSaksbehandler(1, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `oppdater saksbehandler`() {
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(1, SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(0, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `setter nytt timestamp hver gang det lagres`() {
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT, now().minusDays(9))
        assertSisteTidspunkt(now().minusDays(9), SAKSBEHANDLER_OID)
        dao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertSisteTidspunkt(now(), SAKSBEHANDLER_OID)
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

    private fun assertSisteTidspunkt(forventetSisteTidspunkt: LocalDateTime, oid: UUID) {
        @Language("PostgreSQL")
        val query = """
           SELECT siste_handling_utført_tidspunkt FROM saksbehandler WHERE oid = :oid
        """

        val tidspunktFraDb = sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("oid" to oid)).map { it.localDateTime(1) }.asSingle)
        }

        // Sjekk at det er på samme minuttet, for å unngå risikoen for at testen feiler pga. det ble nytt sekund mellom
        // inserten og asserten.
        assertEquals(
            forventetSisteTidspunkt.truncatedTo(ChronoUnit.MINUTES),
            tidspunktFraDb?.truncatedTo(ChronoUnit.MINUTES)
        )
    }
}
