package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.VedtaksperiodeDbDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.mar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgBehandlingApiDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val behandlingDao = PgBehandlingApiDao(dataSource)

    @Test
    fun `Finner periode med oppgave`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettOppgave()
        val vedtaksperiodeMedOppgave = behandlingDao.gjeldendeBehandlingFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet(), emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner periode med oppgave basert på siste behandling`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)

        val nyFom = PERIODE.fom.plusDays(1)
        val nyTom = PERIODE.tom.plusDays(1)
        val spleisBehandlingId = UUID.randomUUID()
        opprettBehandling(fom = nyFom, tom = nyTom, spleisBehandlingId = spleisBehandlingId, fødselsnummer = person.id.value)
        oppdaterSkjæringstidspunkt(spleisBehandlingId, nyFom)
        opprettOppgave()

        val vedtaksperiodeMedOppgave = behandlingDao.gjeldendeBehandlingFor(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, nyFom, nyTom, nyFom, emptySet(), emptySet())
        val ikkeForventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet(), emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
        assertNotEquals(ikkeForventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner alle gjeldende behandlinger for person gitt en oppgaveId`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(fødselsnummer = person.id.value, vedtaksperiodeId = periode2.id, fom = periode2.fom, tom = periode2.tom)
        opprettOppgave()
        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode1 = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet(), emptySet())
        val forventetVedtaksperiode2 = VedtaksperiodeDbDto(periode2.id, periode2.fom, periode2.tom, periode2.fom, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Henter alle behandlinger for person basert på siste behandling, gitt en oppgaveId`() {
        opprettArbeidsgiver()

        val v1 = UUID.randomUUID()
        opprettVedtaksperiode(fødselsnummer = person.id.value, vedtaksperiodeId = v1)
        opprettBehandling(v1, fom = 1 jan 2018, tom = 31 jan 2018, fødselsnummer = person.id.value)
        val spleisBehandlingId1 = UUID.randomUUID()
        opprettBehandling(v1, fom = 1 feb 2018, tom = 28 feb 2018, spleisBehandlingId = spleisBehandlingId1, fødselsnummer = person.id.value)
        oppdaterSkjæringstidspunkt(spleisBehandlingId1, 1 feb 2018)

        val v2 = UUID.randomUUID()
        val periode2 = Periode(v2, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(fødselsnummer = person.id.value, vedtaksperiodeId = periode2.id)
        val spleisBehandlingId2 = UUID.randomUUID()
        opprettBehandling(v2, fom = 1 mar 2018, tom = 31 mar 2018, spleisBehandlingId = spleisBehandlingId2, fødselsnummer = person.id.value)
        oppdaterSkjæringstidspunkt(spleisBehandlingId2, 1 mar 2018)

        val v3 = UUID.randomUUID()
        val periode3 = Periode(v3, LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode3.id, fom = periode3.fom, tom = periode3.tom, fødselsnummer = person.id.value)
        opprettOppgave(vedtaksperiodeId = v3)

        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(finnOppgaveIdFor(periode3.id))

        assertEquals(3, alleVedtaksperioderForPerson.size)
        val forventetVedtaksperiode1 = VedtaksperiodeDbDto(v1, 1 feb 2018, 28 feb 2018, 1 feb 2018, emptySet(), emptySet())
        val forventetVedtaksperiode2 = VedtaksperiodeDbDto(v2, 1 mar 2018, 31 mar 2018, 1 mar 2018, emptySet(), emptySet())
        val forventetVedtaksperiode3 = VedtaksperiodeDbDto(v3, periode3.fom, periode3.tom, periode3.fom, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2, forventetVedtaksperiode3), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke behandlinger for forkastede perioder`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettOppgave()
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode2.id, fom = periode2.fom, tom = periode2.tom, forkastet = true, fødselsnummer = person.id.value)

        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(finnOppgaveIdFor(PERIODE.id))
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke behandlinger for forkastede perioder - med varselSupplier`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettOppgave()
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(vedtaksperiodeId = periode2.id, fom = periode2.fom, tom = periode2.tom, forkastet = true, fødselsnummer = person.id.value)
        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(finnOppgaveIdFor(PERIODE.id)) { emptySet() }
        val forventetVedtaksperiode = VedtaksperiodeDbDto(PERIODE.id, PERIODE.fom, PERIODE.tom, PERIODE.fom, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }

    // Burde bruke VedtaksperiodeDto og lagre via vedtakDao i stedet for manuell update
    private fun oppdaterSkjæringstidspunkt(
        spleisBehandlingId: UUID,
        dato: LocalDate,
    ) {
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
