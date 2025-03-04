package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SpleisBehandlingId
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
        opprettPerson()
        opprettArbeidsgiver()
        opprettBehandling(spleisBehandlingId = spleisBehandlingId, tags = tags)

        // when
        val funnet = repository.finn(SpleisBehandlingId(spleisBehandlingId))

        // then
        assertNotNull(funnet)
        assertEquals(spleisBehandlingId, funnet.id.value)
        assertEquals(tags.toSet(), funnet.tags)
    }
}
