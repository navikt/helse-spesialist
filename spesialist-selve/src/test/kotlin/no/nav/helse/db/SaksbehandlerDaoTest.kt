package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class SaksbehandlerDaoTest: DatabaseIntegrationTest() {
    private val dao = SaksbehandlerDao(dataSource)

    @Test
    fun `lagre saksbehandler`() {
        dao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertSaksbehandler(1, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `oppdater saksbehandler`() {
        dao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        dao.opprettEllerOppdater(SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(1, SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(0, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `setter ikke initielt timestamp`() {
        dao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertSisteTidspunkt(null, SAKSBEHANDLER_OID)
    }

    @Test
    fun `setter tidspunkt når spesialist behandler en handling fra saksbehandleren`() {
        dao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)

        val tidspunkt = now()
        dao.oppdaterSistObservert(SAKSBEHANDLER_OID, tidspunkt)

        assertSisteTidspunkt(tidspunkt, SAKSBEHANDLER_OID)
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

    private fun assertSisteTidspunkt(forventetSisteTidspunkt: LocalDateTime?, oid: UUID) {
        @Language("PostgreSQL")
        val query = """
           SELECT siste_handling_utført_tidspunkt FROM saksbehandler WHERE oid = :oid
        """

        val tidspunktFraDb = sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("oid" to oid)).map { it.localDateTimeOrNull(1) }.asSingle)
        }

        if (forventetSisteTidspunkt != null) assertEquals(
            forventetSisteTidspunkt.truncatedTo(ChronoUnit.MILLIS),
            tidspunktFraDb?.truncatedTo(ChronoUnit.MILLIS)
        )
        else assertNull(tidspunktFraDb)
    }
}
