package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.VedtaksperiodeDbDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class PgBehandlingApiDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
    private val behandling = opprettBehandling(vedtaksperiode)
    private val behandlingDao = PgBehandlingApiDao(dataSource)

    @Test
    fun `Finner periode med oppgave`() {
        val oppgave = opprettOppgave(vedtaksperiode, behandling)
        val vedtaksperiodeMedOppgave = behandlingDao.gjeldendeBehandlingFor(oppgave.id)
        val forventetVedtaksperiode = VedtaksperiodeDbDto(vedtaksperiode.id.value, behandling.fom, behandling.tom, behandling.skjæringstidspunkt, emptySet(), emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner periode med oppgave basert på siste behandling`() {
        val tidligereBehandling = this.behandling
        val nyFom = tidligereBehandling.fom.plusDays(1)
        val nyTom = tidligereBehandling.tom.plusDays(1)
        val nyBehandling = opprettBehandling(vedtaksperiode, fom = nyFom, tom = nyTom, skjæringstidspunkt = nyFom)
        val oppgave = opprettOppgave(vedtaksperiode, nyBehandling)

        val vedtaksperiodeMedOppgave = behandlingDao.gjeldendeBehandlingFor(oppgave.id)
        val forventetVedtaksperiode = VedtaksperiodeDbDto(vedtaksperiode.id.value, nyFom, nyTom, nyFom, emptySet(), emptySet())
        val ikkeForventetVedtaksperiode = VedtaksperiodeDbDto(vedtaksperiode.id.value, tidligereBehandling.fom, tidligereBehandling.tom, tidligereBehandling.skjæringstidspunkt, emptySet(), emptySet())

        assertEquals(forventetVedtaksperiode, vedtaksperiodeMedOppgave)
        assertNotEquals(ikkeForventetVedtaksperiode, vedtaksperiodeMedOppgave)
    }

    @Test
    fun `Finner alle gjeldende behandlinger for person gitt en oppgaveId`() {
        val vedtaksperiode1 = vedtaksperiode
        val behandling1 = behandling
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling2 = opprettBehandling(vedtaksperiode2)
        val oppgave = opprettOppgave(vedtaksperiode1, behandling1)
        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(oppgave.id)
        val forventetVedtaksperiode1 = VedtaksperiodeDbDto(vedtaksperiode1.id.value, behandling1.fom, behandling1.tom, behandling1.skjæringstidspunkt, emptySet(), emptySet())
        val forventetVedtaksperiode2 = VedtaksperiodeDbDto(vedtaksperiode2.id.value, behandling2.fom, behandling2.tom, behandling2.skjæringstidspunkt, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Henter alle behandlinger for person basert på siste behandling, gitt en oppgaveId`() {
        val vedtaksperiode1 = vedtaksperiode
        val behandling1 = behandling
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling2 = opprettBehandling(vedtaksperiode2)
        val vedtaksperiode3 = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling3 = opprettBehandling(vedtaksperiode3)
        val oppgave = opprettOppgave(vedtaksperiode3, behandling3)

        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(oppgave.id)

        assertEquals(3, alleVedtaksperioderForPerson.size)
        val forventetVedtaksperiode1 = VedtaksperiodeDbDto(vedtaksperiode1.id.value, behandling1.fom, behandling1.tom, behandling1.skjæringstidspunkt, emptySet(), emptySet())
        val forventetVedtaksperiode2 = VedtaksperiodeDbDto(vedtaksperiode2.id.value, behandling2.fom, behandling2.tom, behandling2.skjæringstidspunkt, emptySet(), emptySet())
        val forventetVedtaksperiode3 = VedtaksperiodeDbDto(vedtaksperiode3.id.value, behandling3.fom, behandling3.tom, behandling3.skjæringstidspunkt, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode1, forventetVedtaksperiode2, forventetVedtaksperiode3), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke behandlinger for forkastede perioder`() {
        val vedtaksperiode1 = vedtaksperiode
        val behandling1 = behandling
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver, forkastet = true)
        opprettBehandling(vedtaksperiode2)
        val oppgave = opprettOppgave(vedtaksperiode1, behandling1)

        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(oppgave.id)
        val forventetVedtaksperiode = VedtaksperiodeDbDto(vedtaksperiode1.id.value, behandling1.fom, behandling1.tom, behandling1.skjæringstidspunkt, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }

    @Test
    fun `Finner ikke behandlinger for forkastede perioder - med varselSupplier`() {
        val vedtaksperiode1 = vedtaksperiode
        val behandling1 = behandling
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver, forkastet = true)
        opprettBehandling(vedtaksperiode2)
        val oppgave = opprettOppgave(vedtaksperiode1, behandling1)

        val alleVedtaksperioderForPerson = behandlingDao.gjeldendeBehandlingerForPerson(oppgave.id) { emptySet() }
        val forventetVedtaksperiode = VedtaksperiodeDbDto(vedtaksperiode.id.value, behandling1.fom, behandling1.tom, behandling1.skjæringstidspunkt, emptySet(), emptySet())

        assertEquals(setOf(forventetVedtaksperiode), alleVedtaksperioderForPerson)
    }
}
