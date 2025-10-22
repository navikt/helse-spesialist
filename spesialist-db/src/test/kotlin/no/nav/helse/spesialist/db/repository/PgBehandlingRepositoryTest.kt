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
}
