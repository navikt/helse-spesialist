package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgPåVentApiDaoTest : AbstractDBIntegrationTest() {

    private val påVentApiDao = PgPåVentApiDao(dataSource)

    @Test
    fun `henter på vent for aktuell vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettPåVent(vedtaksperiodeId, LocalDate.now().plusDays(21), SAKSBEHANDLER_OID)
        val påvent = påVentApiDao.hentAktivPåVent(vedtaksperiodeId)
        assertNotNull(påvent)
        assertEquals(LocalDate.now().plusDays(21), påvent?.frist)
        assertEquals(SAKSBEHANDLER.id().value, påvent?.oid)
    }

    private fun opprettPåVent(
        vedtaksperiodeId: UUID,
        frist: LocalDate,
        saksbehandlerOid: UUID,
    ) = dbQuery.update(
        "INSERT INTO pa_vent (vedtaksperiode_id, frist, saksbehandler_ref) VALUES (:vedtaksperiodeId, :frist, :oid)",
        "vedtaksperiodeId" to vedtaksperiodeId,
        "frist" to frist,
        "oid" to saksbehandlerOid,
    )

}
