package no.nav.helse.spesialist.api.vedtak

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class ApiVedtakDaoTest: DatabaseIntegrationTest() {

    private val apiVedtakDao = ApiVedtakDao(dataSource)

    @Test
    fun `Finner vedtak med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtakMedOppgave = apiVedtakDao.vedtakFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak = ApiVedtak(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom)

        assertEquals(forventetVedtak, vedtakMedOppgave)
    }

    @Test
    fun `Finner vedtak med oppgave basert på siste generasjon`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val nyFom = PERIODE.fom.plusDays(1)
        val nyTom = PERIODE.tom.plusDays(1)
        nyGenerasjon(vedtaksperiodeId = PERIODE.id, periode = Periode(PERIODE.id, nyFom, nyTom))
        val vedtakMedOppgave = apiVedtakDao.vedtakFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak = ApiVedtak(PERIODE.id, nyFom, nyTom, nyFom)
        val ikkeForventetVedtak = ApiVedtak(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom)

        assertEquals(forventetVedtak, vedtakMedOppgave)
        assertNotEquals(ikkeForventetVedtak, vedtakMedOppgave)
    }

    @Test
    fun `Finner alle vedtak for person gitt en oppgaveId`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        opprettVedtaksperiode(person, arbeidsgiver)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(person, arbeidsgiver, null, periode2)
        val alleVedtakForPerson = apiVedtakDao.alleVedtakForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak1 = ApiVedtak(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom)
        val forventetVedtak2 = ApiVedtak(periode2.id, periode2.fom, periode2.tom, periode2.fom)

        assertEquals(setOf(forventetVedtak1, forventetVedtak2), alleVedtakForPerson)
    }

    @Test
    fun `Hent ut alle vedtak for person gitt en oppgaveId basert på siste generasjon`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()

        val v1 = UUID.randomUUID()
        val periode1 = Periode(v1, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(person, arbeidsgiver, null, periode1)
        nyGenerasjon(v1, periode = Periode(v1, 1.januar, 31.januar))
        nyGenerasjon(v1, periode = Periode(v1, 1.februar, 28.februar))

        val v2 = UUID.randomUUID()
        val periode2 = Periode(v2, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(person, arbeidsgiver, null, periode2)
        nyGenerasjon(v2, periode = Periode(v2, 1.mars, 31.mars))

        val v3 = UUID.randomUUID()
        val periode3 = Periode(v3, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(person, arbeidsgiver, null, periode3)

        val alleVedtakForPerson = apiVedtakDao.alleVedtakForPerson(finnOppgaveIdFor(periode1.id))

        assertEquals(3, alleVedtakForPerson.size)
        val forventetVedtak1 = ApiVedtak(periode1.id, 1.februar, 28.februar, 1.februar)
        val forventetVedtak2 = ApiVedtak(periode2.id, 1.mars, 31.mars, 1.mars)
        val forventetVedtak3 = ApiVedtak(periode3.id, periode3.fom, periode3.tom, periode3.fom)

        assertEquals(setOf(forventetVedtak1, forventetVedtak2, forventetVedtak3), alleVedtakForPerson)
    }
}