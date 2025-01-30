package no.nav.helse.db.api

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import no.nav.helse.util.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgGenerasjonApiDaoTest: DatabaseIntegrationTest() {

    private val generasjonDao = PgGenerasjonApiDao(dataSource)

    @Test
    fun `Finner generasjon med oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val vedtaksperiodeMedOppgave = generasjonDao.gjeldendeGenerasjonFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner generasjon med oppgave basert på siste generasjon`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val nyFom = PERIODE.fom.plusDays(1)
        val nyTom = PERIODE.tom.plusDays(1)
        val spleisBehandlingId = UUID.randomUUID()
        opprettGenerasjon(fom = nyFom, tom = nyTom, spleisBehandlingId = spleisBehandlingId)
        oppdaterSkjæringstidspunkt(spleisBehandlingId, nyFom)
        opprettOppgave()

        val vedtaksperiodeMedOppgave = generasjonDao.gjeldendeGenerasjonFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, nyFom, nyTom, nyFom, emptySet())
        val ikkeForventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
        assertNotEquals(ikkeForventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner alle gjeldende generasjoner for person gitt en oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode2.id, fom = periode2.fom, tom = periode2.tom)
        opprettOppgave()
        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode1 = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())
        val forventetVedtaksperiode2 = VedtaksperiodeDbDto(periode2.id, periode2.fom, periode2.tom, periode2.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Hent ut alle generasjon for person gitt en oppgaveId basert på siste generasjon`() {
        opprettPerson()
        opprettArbeidsgiver()

        val v1 = UUID.randomUUID()
        opprettVedtaksperiode(vedtaksperiodeId = v1)
        opprettGenerasjon(v1, fom = 1.januar, tom = 31.januar)
        val spleisBehandlingId1 = UUID.randomUUID()
        opprettGenerasjon(v1, fom = 1.februar, tom = 28.februar, spleisBehandlingId = spleisBehandlingId1)
        oppdaterSkjæringstidspunkt(spleisBehandlingId1, 1.februar)

        val v2 = UUID.randomUUID()
        val periode2 = Periode(v2, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode2.id)
        val spleisBehandlingId2 = UUID.randomUUID()
        opprettGenerasjon(v2, fom = 1.mars, tom = 31.mars, spleisBehandlingId = spleisBehandlingId2)
        oppdaterSkjæringstidspunkt(spleisBehandlingId2, 1.mars)

        val v3 = UUID.randomUUID()
        val periode3 = Periode(v3, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode3.id, fom = periode3.fom, tom =periode3.tom)
        opprettOppgave(vedtaksperiodeId = v3)

        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(periode3.id))

        assertEquals(3, alleVedtaksperioderForPerson.size)
        val forventetVedtaksperiode1 = VedtaksperiodeDbDto(v1, 1.februar, 28.februar, 1.februar, emptySet())
        val forventetVedtaksperiode2 = VedtaksperiodeDbDto(v2, 1.mars, 31.mars, 1.mars, emptySet())
        val forventetVedtaksperiode3 = VedtaksperiodeDbDto(v3, periode3.fom, periode3.tom, periode3.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2, forventetVedtaksperiode3), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke generasjoner for perioder som er forkastet`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode2.id, fom = periode2.fom, tom = periode2.tom, forkastet = true)

        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke generasjoner for perioder som er forkastet - med varselGetter`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode2.id, fom = periode2.fom, tom = periode2.tom, forkastet = true)
        val alleVedtaksperioderForPerson = generasjonDao.gjeldendeGenerasjonerForPerson(finnOppgaveIdFor(PERIODE.id)) { emptySet() }
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }

    // Burde bruke VedtaksperiodeDto og lagre via vedtakDao i stedet for manuell update
    private fun oppdaterSkjæringstidspunkt(spleisBehandlingId: UUID, dato: LocalDate) {
        dbQuery.update(
            """
            update behandling
            set skjæringstidspunkt = :dato
            where behandling.spleis_behandling_id = :spleisBehandlingId
            """.trimIndent(),
            "dato" to dato,
            "spleisBehandlingId" to spleisBehandlingId,
        )
    }

}
