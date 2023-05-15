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

internal class GenerasjonDaoTest: DatabaseIntegrationTest() {

    private val generasjonDao = GenerasjonDao(dataSource)

    @Test
    fun `Finner generasjon med oppgave`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val vedtaksperiodeMedOppgave = generasjonDao.gjeldendeGenerasjonFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = Vedtaksperiode(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner generasjon med oppgave basert på siste generasjon`() {
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val nyFom = PERIODE.fom.plusDays(1)
        val nyTom = PERIODE.tom.plusDays(1)
        nyGenerasjon(vedtaksperiodeId = PERIODE.id, periode = Periode(PERIODE.id, nyFom, nyTom))
        val vedtaksperiodeMedOppgave = generasjonDao.gjeldendeGenerasjonFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = Vedtaksperiode(PERIODE.id, nyFom, nyTom, nyFom, emptySet())
        val ikkeForventetVedtaksperiode = Vedtaksperiode(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
        assertNotEquals(ikkeForventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner alle gjeldende generasjoner for person gitt en oppgaveId`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        opprettVedtaksperiode(person, arbeidsgiver)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(person, arbeidsgiver, null, periode2)
        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode1 = Vedtaksperiode(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())
        val forventetVedtaksperiode2 = Vedtaksperiode(periode2.id, periode2.fom, periode2.tom, periode2.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Hent ut alle generasjon for person gitt en oppgaveId basert på siste generasjon`() {
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

        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(periode1.id))

        assertEquals(3, alleVedtaksperioderForPerson.size)
        val forventetVedtaksperiode1 = Vedtaksperiode(periode1.id, 1.februar, 28.februar, 1.februar, emptySet())
        val forventetVedtaksperiode2 = Vedtaksperiode(periode2.id, 1.mars, 31.mars, 1.mars, emptySet())
        val forventetVedtaksperiode3 = Vedtaksperiode(periode3.id, periode3.fom, periode3.tom, periode3.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2, forventetVedtaksperiode3), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke generasjoner for perioder som er forkastet`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        opprettVedtaksperiode(person, arbeidsgiver)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(person, arbeidsgiver, null, periode2, forkastet = true)
        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = Vedtaksperiode(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }
}