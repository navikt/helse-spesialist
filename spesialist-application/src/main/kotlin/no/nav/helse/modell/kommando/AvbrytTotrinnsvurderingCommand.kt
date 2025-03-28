package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AvbrytTotrinnsvurderingCommand(
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : Command {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        sikkerlogg.info("setter vedtaksperiode_forkastet i totrinnsvurdering for fødselsnummer=$fødselsnummer")

        val totrinnsvurdering = totrinnsvurderingRepository.finn(fødselsnummer) ?: return true

        totrinnsvurdering.vedtaksperiodeForkastet(vedtaksperiodeId)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}
