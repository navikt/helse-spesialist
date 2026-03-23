package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.loggInfo
import java.util.UUID

internal class AvbrytTotrinnsvurderingCommand(
    private val fødselsnummer: String,
    private val alleForkastedeVedtaksperiodeIder: List<UUID>,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        loggInfo(
            "setter vedtaksperiode_forkastet i totrinnsvurdering for person",
            "fødselsnummer" to fødselsnummer,
        )

        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer) ?: return true

        totrinnsvurdering.vedtaksperiodeForkastet(alleForkastedeVedtaksperiodeIder)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}
