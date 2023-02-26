package no.nav.helse.spesialist.api.varsel

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ApiVarselRepositoryTest: DatabaseIntegrationTest() {

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", status = "INAKTIV", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        val varsler = apiVarselRepository.finnVarslerSomIkkeErInaktiveFor(PERIODE.id, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler for uberegnet periode`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettVarseldefinisjon(kode = "EN_TREDJE_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = PERIODE.id)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_TREDJE_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef, status = "INAKTIV")
        val varsler = apiVarselRepository.finnVarslerForUberegnetPeriode(PERIODE.id)

        assertTrue(varsler.isNotEmpty())
        assertEquals(2, varsler.size)
    }

    @Test
    fun `varsel er aktivt`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        assertEquals(true, apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er ikke aktivt - vurdert`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        assertEquals(false, apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er ikke aktivt - godkjent`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselRepository.godkjennVarslerFor(oppgaveId)
        assertEquals(false, apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `varsel er godkjent`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        apiVarselRepository.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselRepository.godkjennVarslerFor(oppgaveId)
        assertEquals(true, apiVarselRepository.erGodkjent("EN_KODE", generasjonId))
    }

    @Test
    fun `erGodkjent returnerer null hvis varsel ikke finnes`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        assertNull(apiVarselRepository.erGodkjent("EN_KODE", generasjonId))
    }
    @Test
    fun `erAktiv returnerer null hvis varsel ikke finnes`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        opprettVarseldefinisjon(definisjonId = definisjonId, kode = "EN_KODE")
        nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        assertNull(apiVarselRepository.erAktiv("EN_KODE", generasjonId))
    }

    @Test
    fun `tester ikkeVurderteVarslerFor`() {
        val utbetalingId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        opprettVarseldefinisjon(kode = "EN_TREDJE_KODE")
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_TREDJE_KODE", status = "VURDERT", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef)
        val antallIkkeVurderteVarsler = apiVarselRepository.ikkeVurderteVarslerFor(oppgaveId)
        assertEquals(2, antallIkkeVurderteVarsler)
    }

    @Test
    fun `Ingen periode med oppgave - uberegnet periode skal ikke vise varsler`() {
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(null)

        assertTrue(perioderSomSkalViseVarsler.isEmpty())
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = periode("2023-01-02","2023-01-03")
        val periodeMedOppgave = periode("2023-01-04","2023-01-05")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal ikke vise varsler fordi den ikke er tilstøtende perioden med oppgave`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = periode("2023-01-02","2023-01-03")
        val periodeMedOppgave = periode("2023-01-05","2023-01-06")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertFalse(perioderSomSkalViseVarsler.contains(uberegnetPeriode.id))
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende perioden med oppgave (over helg)`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = periode("2023-01-02","2023-01-06")
        val periodeMedOppgave = periode("2023-01-09","2023-01-13")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Uberegnet periode skal vise varsler fordi den er tilstøtende de to andre periodene`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode = periode("2023-01-01","2023-01-02")
        val beregnetPeriode = periode("2023-01-03","2023-01-04")
        val periodeMedOppgave = periode("2023-01-05","2023-01-06")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = beregnetPeriode)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, beregnetPeriode.id, uberegnetPeriode.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Begge uberegnede perioder skal vise varsler fordi de henger sammen med perioden med oppgave`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode1 = periode("2023-01-01","2023-01-02")
        val uberegnetPeriode2 = periode("2023-01-03","2023-01-04")
        val periodeMedOppgave = periode("2023-01-05","2023-01-06")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode1)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode2)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode1.id, uberegnetPeriode2.id), perioderSomSkalViseVarsler)
    }

    @Test
    fun `Ingen uberegnede perioder skal vise varsler fordi de ikke henger sammen med perioden med oppgave`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode1 = periode("2023-01-01","2023-01-02")
        val uberegnetPeriode2 = periode("2023-01-03","2023-01-04")
        val periodeMedOppgave = periode("2023-01-06","2023-01-07")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode1)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode2)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertFalse(perioderSomSkalViseVarsler.any(setOf(uberegnetPeriode1.id, uberegnetPeriode2.id)::contains))
    }

    @Test
    fun `Bare den tilstøtende uberegnede perioden skal vise varsler`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        val uberegnetPeriode1 = periode("2023-01-01","2023-01-02")
        val uberegnetPeriode2 = periode("2023-01-04","2023-01-05")
        val periodeMedOppgave = periode("2023-01-06","2023-01-07")
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periodeMedOppgave, utbetalingId = UUID.randomUUID())
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode1)
        opprettVedtak(personRef, arbeidsgiverRef, periode = uberegnetPeriode2)
        val oppgaveId = finnOppgaveIdFor(periodeMedOppgave.id)
        val perioderSomSkalViseVarsler = apiVarselRepository.perioderSomSkalViseVarsler(oppgaveId)

        assertEquals(setOf(periodeMedOppgave.id, uberegnetPeriode2.id), perioderSomSkalViseVarsler)
    }

    private fun periode(fom: String, tom: String): Periode {
        return Periode(
            UUID.randomUUID(),
            LocalDate.parse(fom),
            LocalDate.parse(tom)
        )
    }
}