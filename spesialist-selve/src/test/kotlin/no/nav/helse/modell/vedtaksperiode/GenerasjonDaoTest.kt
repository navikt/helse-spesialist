package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `finner liste av unike vedtaksperiodeIder med fnr`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonId3 = UUID.randomUUID()

        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId1)
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId3)

        val vedtaksperiodeIder =
            with(generasjonDao) {
                sessionOf(dataSource).use { session ->
                    session.transaction { tx ->
                        tx.finnVedtaksperiodeIderFor(FNR)
                    }
                }
            }
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiodeId1, vedtaksperiodeId2)))
    }
}
