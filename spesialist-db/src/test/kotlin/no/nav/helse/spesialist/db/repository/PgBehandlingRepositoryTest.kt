package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgBehandlingRepositoryTest: AbstractDBIntegrationTest() {
    private val repository = PgBehandlingRepository(session)
    @Test
    fun `finn behandling`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val tags = listOf("FOOBAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            spleisBehandlingId = spleisBehandlingId,
            tags = tags,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finn(SpleisBehandlingId(spleisBehandlingId))

        // then
        assertNotNull(funnet)
        assertEquals(spleisBehandlingId, funnet.id.value)
        assertEquals(tags.toSet(), funnet.tags)
        assertEquals(fødselsnummer, funnet.fødselsnummer)
    }
    @Test
    fun `finn nyeste behandling`() {
        // given
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId1 = UUID.randomUUID()
        val spleisBehandlingId2 = UUID.randomUUID()
        val tags1 = listOf("FOO")
        val tags2 = listOf("BAR")
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId1,
            tags = tags1,
            fødselsnummer = fødselsnummer
        )
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId2,
            tags = tags2,
            fødselsnummer = fødselsnummer
        )

        // when
        val funnet = repository.finnNyeste(vedtaksperiodeId)

        // then
        assertNotNull(funnet)
        assertEquals(spleisBehandlingId2, funnet.id.value)
        assertEquals(tags2.toSet(), funnet.tags)
        assertEquals(fødselsnummer, funnet.fødselsnummer)
    }
}
