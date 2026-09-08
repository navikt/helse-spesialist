package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgLegacyBehandlingDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val behandlingDao = daos.legacyBehandlingDao

    @Test
    fun `finner liste av unike vedtaksperiodeIder med fnr`() {
        val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode1)
        val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)
        opprettBehandling(vedtaksperiode2)

        val vedtaksperiodeIder = behandlingDao.finnVedtaksperiodeIderFor(person.id.value)
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiode1.id.value, vedtaksperiode2.id.value)))
    }

    @Test
    fun `finner vedtaksperiodeider kun for aktuell person`() {
        val person1 = person
        val arbeidsgiver1 = arbeidsgiver

        val person2 = opprettPerson()
        val arbeidsgiver2 = opprettArbeidsgiver()

        val vedtaksperiode1 = opprettVedtaksperiode(person1, arbeidsgiver1)
        opprettBehandling(vedtaksperiode1)
        val vedtaksperiode2 = opprettVedtaksperiode(person2, arbeidsgiver2)
        opprettBehandling(vedtaksperiode2)

        val vedtaksperiodeIderPerson1 = behandlingDao.finnVedtaksperiodeIderFor(person1.id.value)

        val vedtaksperiodeIderPerson2 = behandlingDao.finnVedtaksperiodeIderFor(person2.id.value)
        assertEquals(1, vedtaksperiodeIderPerson1.size)
        assertEquals(1, vedtaksperiodeIderPerson2.size)
        assertTrue(vedtaksperiodeIderPerson1.containsAll(setOf(vedtaksperiode1.id.value)))
        assertTrue(vedtaksperiodeIderPerson2.containsAll(setOf(vedtaksperiode2.id.value)))
    }
}
