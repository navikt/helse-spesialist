package no.nav.helse.modell.vedtaksperiode

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.DefinisjonDao
import no.nav.helse.modell.varsel.Varselmelder
import org.junit.jupiter.api.Test

class ForkastVedtaksperiodeCommandTest {

    private val hendelseId = UUID.randomUUID()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val dao = mockk<VedtakDao>(relaxed = true)
    private val generasjon = mockk<Generasjon>(relaxed = true)
    private val definisjonDao = mockk<DefinisjonDao>()
    private val varselmelder = mockk<Varselmelder>()

    @Test
    fun `markerer vedtaksperiode som forkastet`() {
        val command = ForkastVedtaksperiodeCommand(hendelseId, vedtaksperiodeId, "123", generasjon, dao, definisjonDao, varselmelder)
        command.execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { dao.markerForkastet(vedtaksperiodeId, hendelseId) }
    }

    @Test
    fun `avviser varsler for siste generasjon av vedtaksperiode`() {
        val command = ForkastVedtaksperiodeCommand(hendelseId, vedtaksperiodeId, "123", generasjon, dao, definisjonDao, varselmelder)
        command.execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { generasjon.avvisVarsler() }
    }
}
