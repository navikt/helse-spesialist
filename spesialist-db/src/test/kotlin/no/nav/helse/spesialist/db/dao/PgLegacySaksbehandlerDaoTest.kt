package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class PgLegacySaksbehandlerDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `lagre saksbehandler`() {
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertSaksbehandler(skalFinnesIDatabasen = true, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `oppdater saksbehandler`() {
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(skalFinnesIDatabasen = true, SAKSBEHANDLER_OID, "ANNET_NAVN", "ANNEN_EPOST", "ANNEN_IDENT")
        assertSaksbehandler(skalFinnesIDatabasen = false, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `setter ikke initielt timestamp`() {
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertSisteTidspunkt(null, SAKSBEHANDLER_OID)
    }

    @Test
    fun `setter tidspunkt når spesialist behandler en handling fra saksbehandleren`() {
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)

        val tidspunkt = now()
        saksbehandlerDao.oppdaterSistObservert(SAKSBEHANDLER_OID, tidspunkt)

        assertSisteTidspunkt(tidspunkt, SAKSBEHANDLER_OID)
    }

    private fun assertSaksbehandler(
        skalFinnesIDatabasen: Boolean, oid: UUID, navn: String, epost: String, ident: String
    ) {
        val erLagret = dbQuery.singleOrNull(
            """
            SELECT 1 FROM saksbehandler
            WHERE oid = :oid AND navn = :navn AND epost = :epost AND ident = :ident
            """.trimIndent(), "oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident
        ) { true } ?: false

        assertEquals(skalFinnesIDatabasen, erLagret)
    }

    private fun assertSisteTidspunkt(forventetSisteTidspunkt: LocalDateTime?, oid: UUID) {
        val tidspunktFraDb = dbQuery.singleOrNull(
            """
            SELECT siste_handling_utført_tidspunkt FROM saksbehandler WHERE oid = :oid
            """.trimIndent(), "oid" to oid
        ) { it.localDateTimeOrNull(1) }

        if (forventetSisteTidspunkt != null) assertEquals(
            forventetSisteTidspunkt.truncatedTo(ChronoUnit.MILLIS), tidspunktFraDb?.truncatedTo(ChronoUnit.MILLIS)
        )
        else assertNull(tidspunktFraDb)
    }
}
