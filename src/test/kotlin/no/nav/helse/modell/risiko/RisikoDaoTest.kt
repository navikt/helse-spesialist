package no.nav.helse.modell.risiko

import AbstractEndToEndTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertNotNull

internal class RisikoDaoTest : AbstractEndToEndTest() {

    @Test
    fun `les og skriv`() {
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.persisterRisikovurdering(RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
            samletScore = 10.0,
            faresignaler = listOf("Signal1", "Signal2"),
            arbeidsuførhetvurdering = listOf("Vurdering1", "Vurdering2"),
            ufullstendig = true
        ))

        val vurdering = risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
        assertNotNull(vurdering)
    }
}
