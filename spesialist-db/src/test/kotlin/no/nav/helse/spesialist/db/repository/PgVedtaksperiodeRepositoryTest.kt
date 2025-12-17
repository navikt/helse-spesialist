package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgVedtaksperiodeRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.vedtaksperiodeRepository

    @Test
    fun `finn vedtaksperiode`() {
        // given
        val vedtaksperiodeId = UUID.randomUUID()
        val fødselsnummer = opprettPerson().id.value
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
        )

        // when
        val funnet = repository.finn(VedtaksperiodeId(vedtaksperiodeId))

        // then
        assertNotNull(funnet)
        assertEquals(vedtaksperiodeId, funnet.id.value)
        assertEquals(fødselsnummer, funnet.fødselsnummer)
    }
}
