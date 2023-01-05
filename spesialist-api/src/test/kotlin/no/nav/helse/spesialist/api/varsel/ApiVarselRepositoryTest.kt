package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ApiVarselRepositoryTest: DatabaseIntegrationTest() {

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "INAKTIV")
        val varsler = apiVarselRepository.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(1, varsler.size)
    }
}