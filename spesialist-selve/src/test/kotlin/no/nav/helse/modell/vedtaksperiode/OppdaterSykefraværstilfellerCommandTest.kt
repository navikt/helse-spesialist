package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OppdaterSykefraværstilfellerCommandTest {

    @Test
    fun `oppdaterer sykefraværstilfelle`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonsId = UUID.randomUUID()
        val generasjon = generasjon(generasjonsId, vedtaksperiodeId)
        val vedtaksperiodeOppdateringer = listOf(
            VedtaksperiodeOppdatering(1.februar, 5.februar, 1.februar, vedtaksperiodeId)
        )
        generasjon.registrer(observer)

        val command = OppdaterSykefraværstilfellerCommand("fnr", "aktørId", vedtaksperiodeOppdateringer, listOf(generasjon), UUID.randomUUID())
        command.execute(CommandContext(UUID.randomUUID()))

        assertEquals(1, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonsId, observer.oppdaterteGenerasjoner[0])
    }

    private fun generasjon(generasjonId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )


    private val observer = object : IVedtaksperiodeObserver {
        val oppdaterteGenerasjoner = mutableListOf<UUID>()
        override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
            oppdaterteGenerasjoner.add(generasjonId)
        }
    }
}