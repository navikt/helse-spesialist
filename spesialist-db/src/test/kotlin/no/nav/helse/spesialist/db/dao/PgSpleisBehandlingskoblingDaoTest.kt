package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.Varsel
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class PgSpleisBehandlingskoblingDaoTest : AbstractDBIntegrationTest() {
    private val dao = sessionContext.spleisBehandlingskoblingDao
    private val vedtaksperiode = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

    @Test
    fun `finner behandlingene til vedtaksperioden`() {
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId), spleisBehandlingId = SpleisBehandlingId(spleisBehandlingId))
        opprettBehandling(vedtaksperiode, utbetalingId = null, spleisBehandlingId = null)

        val behandlinger = dao.finnBehandlinger(vedtaksperiode.id.value).sortedBy { it.id }

        assertEquals(2, behandlinger.size)
        assertEquals(utbetalingId, behandlinger[0].utbetalingId)
        assertEquals(spleisBehandlingId, behandlinger[0].spleisBehandlingId)
        assertNull(behandlinger[1].utbetalingId)
        assertNull(behandlinger[1].spleisBehandlingId)
    }

    @Test
    fun `setter spleis-id på varsler som mangler den`() {
        val behandling = opprettBehandling(vedtaksperiode, spleisBehandlingId = null)
        val varsel1 = opprettVarsel(behandling, "RV_IV_1")
        val varsel2 = opprettVarsel(behandling, "RV_IV_2")
        val rad = dao.finnBehandlinger(vedtaksperiode.id.value).single()
        val spleisBehandlingId = UUID.randomUUID()

        assertEquals(2, dao.antallVarslerUtenSpleisBehandlingId(rad.id))
        assertEquals(2, dao.settSpleisBehandlingIdPåVarsler(rad.id, spleisBehandlingId))

        assertEquals(spleisBehandlingId, spleisBehandlingIdPå(varsel1))
        assertEquals(spleisBehandlingId, spleisBehandlingIdPå(varsel2))
        assertEquals(0, dao.antallVarslerUtenSpleisBehandlingId(rad.id))
        assertEquals(0, dao.settSpleisBehandlingIdPåVarsler(rad.id, UUID.randomUUID()))
        assertEquals(spleisBehandlingId, spleisBehandlingIdPå(varsel1))
    }

    @Test
    fun `overskriver ikke spleis-id som alt står på varselet`() {
        val behandling = opprettBehandling(vedtaksperiode)
        val varsel = opprettVarsel(behandling, "RV_IV_1")
        val rad = dao.finnBehandlinger(vedtaksperiode.id.value).single()

        assertEquals(0, dao.settSpleisBehandlingIdPåVarsler(rad.id, UUID.randomUUID()))
        assertEquals(behandling.spleisBehandlingId?.value, spleisBehandlingIdPå(varsel))
    }

    private fun spleisBehandlingIdPå(varsel: Varsel): UUID? =
        dbQuery.singleOrNull(
            "SELECT spleis_behandling_id FROM varsel WHERE unik_id = :unikId",
            "unikId" to varsel.id.value,
        ) { it.uuidOrNull("spleis_behandling_id") }
}
