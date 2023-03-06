package no.nav.helse.spesialist.api.varsel

import java.time.LocalDate
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `Finner varsler som skal med for siste snapshotgenerasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val generasjonRef3 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef3)
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(3, varsler.size)
    }

    @Test
    fun `Finner aktive varsler for uberegnet periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        val varsler = apiVarselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId)

        assertEquals(4, varsler.size)
    }

    @Test
    fun `Finner godkjente varsler for uberegnet periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", status = "GODKJENT", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        val varsler = apiVarselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId)

        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler med oppgaveId`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
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
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId)
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId2, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        val forventetVarsel1 = Varsel(generasjonId1, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)
        val forventetVarsel2 = Varsel(generasjonId2, definisjonId,"EN_KODE", "EN_TITTEL", null, null, null)
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(setOf(forventetVarsel1, forventetVarsel2), varsler)
    }

    @Test
    fun `Finner varsler som ikke er inaktive for siste generasjon av en liste vedtaksperioder`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personRef, arbeidsgiverRef)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(personRef, arbeidsgiverRef, null, periode2)
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE", definisjonId = definisjonId1)
        opprettVarseldefinisjon(kode = "EN_TREDJE_KODE")
        opprettVarseldefinisjon(kode = "EN_FJERDE_KODE", definisjonId = definisjonId2)
        val førsteGenerasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id)
        val sisteGenerasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1)
        val førsteGenerasjonRef2 = nyGenerasjon(vedtaksperiodeId = periode2.id)
        val sisteGenerasjonRef2 = nyGenerasjon(vedtaksperiodeId = periode2.id, generasjonId = generasjonId2)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = førsteGenerasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = periode2.id, generasjonRef = førsteGenerasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = sisteGenerasjonRef1)
        nyttVarsel(kode = "EN_TREDJE_KODE", status = "INAKTIV", vedtaksperiodeId = PERIODE.id, generasjonRef = sisteGenerasjonRef1)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = periode2.id, generasjonRef = sisteGenerasjonRef2)
        nyttVarsel(kode = "EN_FJERDE_KODE", vedtaksperiodeId = periode2.id, generasjonRef = sisteGenerasjonRef2)

        val forventetVarsel1 = Varsel(generasjonId1, definisjonId1,"EN_ANNEN_KODE", "EN_TITTEL", null, null, null)
        val forventetVarsel2 = Varsel(generasjonId2, definisjonId1,"EN_ANNEN_KODE", "EN_TITTEL", null, null, null)
        val forventetVarsel3 = Varsel(generasjonId2, definisjonId2,"EN_FJERDE_KODE", "EN_TITTEL", null, null, null)

        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(listOf(PERIODE.id, periode2.id))

        assertEquals(setOf(forventetVarsel1, forventetVarsel2, forventetVarsel3), varsler)
    }

    @Test
    fun `Godkjenner vurderte varsler for en liste vedtaksperioder`() {
        val utbetalingId = UUID.randomUUID()
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personId = personRef, arbeidsgiverId = arbeidsgiverRef, utbetalingId = utbetalingId)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(personRef, arbeidsgiverRef, null, periode2)
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId1)
        opprettVarseldefinisjon(kode = "SB_BO_1234", definisjonId = definisjonId2)
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = periode2.id, generasjonId = generasjonId2, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "SB_BO_1234", vedtaksperiodeId = periode2.id, generasjonRef = generasjonRef2)
        apiVarselDao.settStatusVurdert(generasjonId1, definisjonId1, "EN_KODE", "EN_IDENT")
        apiVarselDao.settStatusVurdertPåBeslutteroppgavevarsler(oppgaveId, "EN_IDENT")
        val forventetVarsel1 = Varsel(generasjonId1, definisjonId1,"EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), GODKJENT))
        val forventetVarsel2 = Varsel(generasjonId2, definisjonId2,"SB_BO_1234", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), GODKJENT))
        apiVarselDao.godkjennVarslerFor(listOf(PERIODE.id, periode2.id))
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(setOf(forventetVarsel1, forventetVarsel2), varsler)
    }


    @Test
    fun `Godkjenner alle varsler med samme utbetalingId gitt oppgaveId`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
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

        assertEquals(setOf(forventetVarsel1, forventetVarsel2), varsler)
    }

    @Test
    fun `Lagrer vurdert besluttervarsel med siste definisjon for varselkoden`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "SB_BO_1234", definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "SB_BO_1234", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        apiVarselDao.settStatusVurdertPåBeslutteroppgavevarsler(oppgaveId, "EN_IDENT")
        opprettVarseldefinisjon(kode = "SB_BO_1234")
        val forventetVarsel = Varsel(generasjonId, definisjonId,"SB_BO_1234", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), VURDERT))
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(setOf(forventetVarsel), varsler)
    }

    @Test
    fun `Setter alle varsler med samme utbetalingId som ikke er inaktive til vurdert gitt oppgaveId`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId1)
        opprettVarseldefinisjon(kode = "SB_BO_1234", definisjonId = definisjonId2)
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId2, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "SB_BO_1234", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2, status = "INAKTIV")
        apiVarselDao.settStatusVurdertFor(oppgaveId, "EN_IDENT")
        val forventetVarsel1 = Varsel(generasjonId1, definisjonId1,"EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), VURDERT))
        val forventetVarsel2 = Varsel(generasjonId2, definisjonId2,"SB_BO_1234", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), VURDERT))
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)

        assertEquals(setOf(forventetVarsel1, forventetVarsel2), varsler)
    }

    @Test
    fun `Godkjenner ikke varsler med ulik utbetalingId gitt oppgaveId`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
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
    fun `kan ikke sette varsel til vurdert dersom det allerede er vurdert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef)
        val oppdatertVarsel = apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        val forsøktOppdatertVarsel = apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")

        assertNotNull(oppdatertVarsel)
        assertEquals(
            Varsel(generasjonId, definisjonId, "EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), VURDERT)),
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
        assertNull(forsøktOppdatertVarsel)
    }

    @Test
    fun `kan ikke sette varsel til vurdert dersom det er godkjent`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)

        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef)
        val oppdatertVarsel = apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselDao.godkjennVarslerFor(oppgaveId)
        val forsøktOppdatertVarsel = apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")

        assertNotNull(oppdatertVarsel)
        assertEquals(
            Varsel(generasjonId, definisjonId, "EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), GODKJENT)),
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
        assertNull(forsøktOppdatertVarsel)
    }

    @Test
    fun `kan ikke sette varsel til AKTIV dersom det allerede er GODKJENT`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef, status = "GODKJENT")
        val forsøktOppdatertVarsel = apiVarselDao.settStatusAktiv(generasjonId, "EN_KODE", "EN_IDENT")

        assertEquals(
            Varsel(generasjonId, definisjonId, "EN_KODE", "EN_TITTEL", null, null, Varselvurdering("EN_IDENT", LocalDateTime.now(), GODKJENT)),
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
        assertNull(forsøktOppdatertVarsel)
    }

    @Test
    fun `setter status til VURDERT for beslutteroppgavevarsler`() {
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(personId = opprettPerson(), arbeidsgiverId = opprettArbeidsgiver(), utbetalingId = utbetalingId)
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
}