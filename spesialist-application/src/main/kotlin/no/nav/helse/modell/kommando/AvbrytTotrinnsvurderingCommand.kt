package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import java.util.UUID

internal class AvbrytTotrinnsvurderingCommand(
    private val fødselsnummer: String,
    private val alleForkastedeVedtaksperiodeIder: List<UUID>,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        loggInfo(
            "setter vedtaksperiode_forkastet i totrinnsvurdering for person",
            "fødselsnummer" to fødselsnummer,
        )

        val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer) ?: return true

        totrinnsvurdering.vedtaksperiodeForkastet(alleForkastedeVedtaksperiodeIder)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}
