package no.nav.helse.spesialist.api.vedtak

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class ApiVedtakDaoTest: DatabaseIntegrationTest() {

    private val apiVedtakDao = ApiVedtakDao(dataSource)

    @Test
    fun `Finner vedtak med oppgave`() {
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtakMedOppgave = apiVedtakDao.vedtakFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak = ApiVedtak(vedtakRef, PERIODE.id, PERIODE.fom, PERIODE.tom)

        assertEquals(forventetVedtak, vedtakMedOppgave)
    }

    @Test
    fun `Finner vedtak med oppgave basert på siste generasjon`() {
        val vedtakRef = opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val nyFom = PERIODE.fom.plusDays(1)
        val nyTom = PERIODE.tom.plusDays(1)
        nyGenerasjon(vedtaksperiodeId = PERIODE.id, periode = Periode(PERIODE.id, nyFom, nyTom))
        val vedtakMedOppgave = apiVedtakDao.vedtakFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak = ApiVedtak(vedtakRef, PERIODE.id, nyFom, nyTom)
        val ikkeForventetVedtak = ApiVedtak(vedtakRef, PERIODE.id, PERIODE.fom, PERIODE.tom)

        assertEquals(forventetVedtak, vedtakMedOppgave)
        assertNotEquals(ikkeForventetVedtak, vedtakMedOppgave)
    }

    @Test
    fun `Finner alle vedtak for person gitt en oppgaveId`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtakRef1 = opprettVedtaksperiode(person, arbeidsgiver)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        val vedtakRef2 = opprettVedtaksperiode(person, arbeidsgiver, null, periode2)
        val alleVedtakForPerson = apiVedtakDao.alleVedtakForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak1 = ApiVedtak(vedtakRef1, PERIODE.id, PERIODE.fom, PERIODE.tom)
        val forventetVedtak2 = ApiVedtak(vedtakRef2, periode2.id, periode2.fom, periode2.tom)

        assertEquals(setOf(forventetVedtak1, forventetVedtak2), alleVedtakForPerson)
    }

}