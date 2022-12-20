package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.VURDERT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselvurdering
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ApiVarselDaoTest: DatabaseIntegrationTest() {

    private val apiVarselDao = ApiVarselDao(dataSource)

    @Test
    fun `Tom liste ved manglende varsler`() {
        assertTrue(apiVarselDao.finnVarslerFor(PERIODE.id, UUID.randomUUID()).isEmpty())
    }

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val varsler = apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(2, varsler.size)
    }

    @Test
    fun `Finner varsler med oppgaveId`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetlingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        val forventetVarsel = Varsel(generasjonId, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)
        val varsler = apiVarselDao.finnVarslerFor(oppgaveId)

        checkNotNull(varsler)
        assertEquals(forventetVarsel, varsler.single())
    }

    @Test
    fun `Finner siste definisjon for varsler når definisjonsRef ikke finnes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        opprettVarseldefinisjon(tittel = "EN_NY_TITTEL", definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val forventetVarsel = Varsel(generasjonId, definisjonId,"EN_KODE", "EN_NY_TITTEL", null, null, null)

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).single())
    }

    @Test
    fun `Finner riktig definisjon for varsler når varselet har definisjonRef`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL", definisjonId = definisjonId)
        opprettVarseldefinisjon("EN_NY_TITTEL")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef)
        val forventetVarsel = Varsel(generasjonId, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).single())
    }

    @Test
    fun `setter status til VURDERT`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef)
        val forventetVarsel = Varsel(generasjonId, definisjonId,"EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), VURDERT))
        apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).single())
    }

    @Test
    fun `setter status til AKTIV`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef)
        val forventetVarsel = Varsel(generasjonId, definisjonId,"EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), AKTIV))
        apiVarselDao.settStatusAktiv(generasjonId, "EN_KODE", "EN_IDENT")

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).single())
    }

    private fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT o.id FROM oppgave o JOIN vedtak v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;"
        return requireNotNull(session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.long("id") }.asSingle))
    }
}