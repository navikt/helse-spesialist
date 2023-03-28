package no.nav.helse.modell.vedtaksperiode

import io.mockk.mockk
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OppdaterSykefraværstilfellerCommandTest {

    private val repository = mockk<ActualGenerasjonRepository>(relaxed = true)

    @Test
    fun `oppdaterer sykefraværstilfelle`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonsId = UUID.randomUUID()
        val skjæringstidspunkt = 1.januar
        val generasjon = Generasjon(generasjonsId, vedtaksperiodeId, repository)
        val vedtaksperiodeOppdateringer = listOf(
            VedtaksperiodeOppdatering(skjæringstidspunkt, 5.januar, skjæringstidspunkt, vedtaksperiodeId)
        )
        val vedtaksperiode = Vedtaksperiode(vedtaksperiodeId, generasjon)
        vedtaksperiode.registrer(observer)
        val vedtaksperioder = listOf(vedtaksperiode)

        val command = OppdaterSykefraværstilfellerCommand("fnr", "aktørId", vedtaksperiodeOppdateringer, vedtaksperioder)
        command.execute(CommandContext(UUID.randomUUID()))

        assertEquals(1, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonsId, observer.oppdaterteGenerasjoner[0])
    }

    private val observer = object : IVedtaksperiodeObserver {
        val oppdaterteGenerasjoner = mutableListOf<UUID>()
        override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
            oppdaterteGenerasjoner.add(generasjonId)
        }
    }
}