package no.nav.helse.db.api

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PåVentApiDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `henter på vent for aktuell vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettPåVent(vedtaksperiodeId)
        val påvent = påVentApiDao.hentAktivPåVent(vedtaksperiodeId)
        assertNotNull(påvent)
        assertEquals(LocalDate.now().plusDays(21), påvent?.frist)
        assertEquals(SAKSBEHANDLER.oid, påvent?.oid)
    }
}
