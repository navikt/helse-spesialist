package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
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
        assertTrue(apiVarselDao.finnVarslerSomIkkeErInaktiveFor(PERIODE.id, UUID.randomUUID()).isEmpty())
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
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId)

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
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(forventetVarsel, varsler.single())
    }

    @Test
    fun `Finner alle varsler med samme utbetalingId gitt oppgaveId`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetlingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId)
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId2, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        val forventetVarsel1 = Varsel(generasjonId1, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)
        val forventetVarsel2 = Varsel(generasjonId2, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(listOf(forventetVarsel1, forventetVarsel2), varsler)
    }

    @Test
    fun `Godkjenner alle varsler med samme utbetalingId gitt oppgaveId`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetlingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId1)
        opprettVarseldefinisjon(kode = "SB_BO_1234", definisjonId = definisjonId2)
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId2, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "SB_BO_1234", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        apiVarselDao.settStatusVurdert(generasjonId1, definisjonId1, "EN_KODE", "EN_IDENT")
        apiVarselDao.settStatusVurdertPåBeslutteroppgavevarsler(oppgaveId, "EN_IDENT")
        val forventetVarsel1 = Varsel(generasjonId1, definisjonId1,"EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), GODKJENT))
        val forventetVarsel2 = Varsel(generasjonId2, definisjonId2,"SB_BO_1234", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), GODKJENT))
        apiVarselDao.godkjennVarslerFor(oppgaveId)
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(listOf(forventetVarsel1, forventetVarsel2), varsler)
    }

    @Test
    fun `Godkjenner ikke varsler med ulik utbetalingId gitt oppgaveId`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetlingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId)
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = UUID.randomUUID())
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselDao.godkjennVarslerFor(oppgaveId)
        val antallGodkjenteVarsler = finnVarslerFor(GODKJENT)

        assertEquals(1, antallGodkjenteVarsler)
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

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single())
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

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single())
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

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single())
    }

    @Test
    fun `setter status til VURDERT for beslutteroppgavevarsler`() {
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetlingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = UUID.randomUUID(), utbetalingId = utbetalingId)
        nyttVarsel(kode = "SB_BO_1234", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        nyttVarsel(kode = "NOE_ANNET", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        apiVarselDao.settStatusVurdertPåBeslutteroppgavevarsler(oppgaveId, "EN_IDENT")
        val antallVurderteVarsler = finnVarslerFor(VURDERT)

        assertEquals(1, antallVurderteVarsler)
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
        val forventetVarsel = Varsel(generasjonId, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)
        apiVarselDao.settStatusAktiv(generasjonId, "EN_KODE", "EN_IDENT")

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single())
    }

    private fun finnVarslerFor(status: Varselstatus): Int = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT count(1) FROM selve_varsel WHERE status = :status;"
        return requireNotNull(session.run(queryOf(query, mapOf("status" to status.name)).map { it.int(1) }.asSingle))
    }

    private fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT o.id FROM oppgave o JOIN vedtak v ON v.id = o.vedtak_ref WHERE v.vedtaksperiode_id = :vedtaksperiode_id;"
        return requireNotNull(session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.long("id") }.asSingle))
    }
}