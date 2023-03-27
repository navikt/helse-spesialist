package no.nav.helse.modell.vedtaksperiode

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Test

class OppdaterSykefraværstilfellerCommandTest {

    private val repository = mockk<ActualGenerasjonRepository>(relaxed = true)

    @Test
    fun `oppdaterer sykefraværstilfelle`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonsId = UUID.randomUUID()
        val skjæringstidspunkt = 1.januar
        val periode = Periode(skjæringstidspunkt, 5.januar)
        every { repository.finnÅpenGenerasjonFor(any()) } returns Generasjon(generasjonsId, vedtaksperiodeId, repository)
        val vedtaksperioder = listOf(VedtaksperiodeOppdatering(vedtaksperiodeId = vedtaksperiodeId, skjæringstidspunkt = skjæringstidspunkt, fom = skjæringstidspunkt, tom = 5.januar))
        val command = OppdaterSykefraværstilfellerCommand("fnr", "aktørId", vedtaksperioder, repository)
        command.execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { repository.finnÅpenGenerasjonFor(vedtaksperiodeId) }
        verify(exactly = 1) { repository.oppdaterSykefraværstilfelle(generasjonsId, skjæringstidspunkt, periode) }
    }
}