package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun `varsel er aktivt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        assertTrue(apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er ikke aktivt - vurdert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        assertFalse(apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er ikke aktivt - godkjent`() {
        val vedtaksperiodeId = PERIODE.id
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselRepository.godkjennVarslerFor(oppgaveId)
        assertFalse(apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    private fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT o.id FROM oppgave o JOIN vedtak v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;"
        return requireNotNull(session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.long("id") }.asSingle))
    }
}