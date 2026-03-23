package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
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
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT.value)
        assertSaksbehandler(skalFinnesIDatabasen = true, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `oppdater saksbehandler`() {
        val saksbehandler = lagSaksbehandler()
        saksbehandlerDao.opprettEllerOppdater(saksbehandler.id.value, saksbehandler.navn, saksbehandler.epost, saksbehandler.ident.value)
        val oppdatertSaksbehandler = lagSaksbehandler(id = saksbehandler.id)
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, oppdatertSaksbehandler.navn, oppdatertSaksbehandler.epost, oppdatertSaksbehandler.ident.value)
        assertSaksbehandler(skalFinnesIDatabasen = true, SAKSBEHANDLER_OID, oppdatertSaksbehandler.navn, oppdatertSaksbehandler.epost, oppdatertSaksbehandler.ident)
        assertSaksbehandler(skalFinnesIDatabasen = false, SAKSBEHANDLER_OID, saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)
    }

    @Test
    fun `setter ikke initielt timestamp`() {
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT.value)
        assertSisteTidspunkt(null, SAKSBEHANDLER_OID)
    }

    @Test
    fun `setter tidspunkt når spesialist behandler en handling fra saksbehandleren`() {
        saksbehandlerDao.opprettEllerOppdater(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT.value)

        val tidspunkt = now()
        saksbehandlerDao.oppdaterSistObservert(SAKSBEHANDLER_OID, tidspunkt)

        assertSisteTidspunkt(tidspunkt, SAKSBEHANDLER_OID)
    }

    private fun assertSaksbehandler(
        skalFinnesIDatabasen: Boolean,
        oid: UUID,
        navn: String,
        epost: String,
        ident: NAVIdent,
    ) {
        val erLagret =
            dbQuery.singleOrNull(
                """
                SELECT 1 FROM saksbehandler
                WHERE oid = :oid AND navn = :navn AND epost = :epost AND ident = :ident
                """.trimIndent(),
                "oid" to oid,
                "navn" to navn,
                "epost" to epost,
                "ident" to ident.value,
            ) { true } ?: false

        assertEquals(skalFinnesIDatabasen, erLagret)
    }

    private fun assertSisteTidspunkt(
        forventetSisteTidspunkt: LocalDateTime?,
        oid: UUID,
    ) {
        val tidspunktFraDb =
            dbQuery.singleOrNull(
                """
                SELECT siste_handling_utført_tidspunkt FROM saksbehandler WHERE oid = :oid
                """.trimIndent(),
                "oid" to oid,
            ) { it.localDateTimeOrNull(1) }

        if (forventetSisteTidspunkt != null) {
            assertEquals(
                forventetSisteTidspunkt.truncatedTo(ChronoUnit.MILLIS),
                tidspunktFraDb?.truncatedTo(ChronoUnit.MILLIS),
            )
        } else {
            assertNull(tidspunktFraDb)
        }
    }
}
