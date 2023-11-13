package no.nav.helse.modell.vedtaksperiode

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Test

class ForkastVedtaksperiodeCommandTest {

    private val hendelseId = UUID.randomUUID()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val dao = mockk<VedtakDao>(relaxed = true)
    private val generasjon = mockk<Generasjon>(relaxed = true)

    @Test
    fun `markerer vedtaksperiode som forkastet`() {
        val command = ForkastVedtaksperiodeCommand(hendelseId, vedtaksperiodeId, generasjon, dao)
        command.execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { dao.markerForkastet(vedtaksperiodeId, hendelseId) }
    }

    @Test
    fun `avviser varsler for siste generasjon av vedtaksperiode`() {
        val command = ForkastVedtaksperiodeCommand(hendelseId, vedtaksperiodeId, generasjon, dao)
        command.execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { generasjon.avvisVarsler() }
    }
}