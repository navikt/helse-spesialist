package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.util.UUID

internal class AvbrytTotrinnsvurderingCommand(
    private val identitetsnummer: Identitetsnummer,
    private val alleForkastedeVedtaksperiodeIder: List<UUID>,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        loggInfo(
            "setter vedtaksperiode_forkastet i totrinnsvurdering for person",
            "fødselsnummer" to identitetsnummer.value,
        )

        val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(identitetsnummer.value) ?: return true

        totrinnsvurdering.vedtaksperiodeForkastet(alleForkastedeVedtaksperiodeIder)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}
