package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory

internal class ForkastVedtaksperiodeCommand(
    private val hendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val vedtakDao: VedtakDao
): Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(ForkastVedtaksperiodeCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Markerer {} som forkastet", kv("vedtaksperiodeId", vedtaksperiodeId))
        vedtakDao.markerForkastet(vedtaksperiodeId, hendelseId)
        return true
    }
}