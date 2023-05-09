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

internal class ApiGenerasjonDaoTest: DatabaseIntegrationTest() {

    private val apiGenerasjonDao = ApiGenerasjonDao(dataSource)

    @Test
    fun `Finner vedtak med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtakMedOppgave = apiGenerasjonDao.gjeldendeGenerasjonFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak = ApiGenerasjon(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(forventetVedtak, vedtakMedOppgave)
    }

    @Test
    fun `Finner vedtak med oppgave basert på siste generasjon`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val nyFom = PERIODE.fom.plusDays(1)
        val nyTom = PERIODE.tom.plusDays(1)
        nyGenerasjon(vedtaksperiodeId = PERIODE.id, periode = Periode(PERIODE.id, nyFom, nyTom))
        val vedtakMedOppgave = apiGenerasjonDao.gjeldendeGenerasjonFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak = ApiGenerasjon(PERIODE.id, nyFom, nyTom, nyFom, emptySet())
        val ikkeForventetVedtak = ApiGenerasjon(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

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
        val alleVedtakForPerson = apiGenerasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtak1 = ApiGenerasjon(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())
        val forventetVedtak2 = ApiGenerasjon(periode2.id, periode2.fom, periode2.tom, periode2.fom, emptySet())

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

        val alleVedtakForPerson = apiGenerasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(periode1.id))

        assertEquals(3, alleVedtakForPerson.size)
        val forventetVedtak1 = ApiGenerasjon(periode1.id, 1.februar, 28.februar, 1.februar, emptySet())
        val forventetVedtak2 = ApiGenerasjon(periode2.id, 1.mars, 31.mars, 1.mars, emptySet())
        val forventetVedtak3 = ApiGenerasjon(periode3.id, periode3.fom, periode3.tom, periode3.fom, emptySet())

        assertEquals(setOf(forventetVedtak1, forventetVedtak2, forventetVedtak3), alleVedtakForPerson)
    }
}