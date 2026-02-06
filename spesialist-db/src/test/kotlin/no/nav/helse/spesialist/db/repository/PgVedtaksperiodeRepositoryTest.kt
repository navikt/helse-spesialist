package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PgVedtaksperiodeRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.vedtaksperiodeRepository

    @Test
    fun `finn vedtaksperiode`() {
        // given
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode =
            opprettVedtaksperiode(person, arbeidsgiver).also {
                opprettBehandling(it)
            }

        // when
        val funnet = repository.finn(vedtaksperiode.id)

        // then
        assertNotNull(funnet)
        assertEquals(vedtaksperiode.id, funnet.id)
        assertEquals(person.id, funnet.identitetsnummer)
    }

    @Test
    fun `lagre forkastet vedtaksperiode`() {
        // given
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode =
            opprettVedtaksperiode(person, arbeidsgiver, forkastet = true).also {
                opprettBehandling(it)
            }

        // when
        val funnet = repository.finn(vedtaksperiode.id)

        // then
        assertNotNull(funnet)
        assertEquals(vedtaksperiode.id, funnet.id)
        assertEquals(person.id, funnet.identitetsnummer)
        assertTrue(funnet.forkastet)
    }
}
