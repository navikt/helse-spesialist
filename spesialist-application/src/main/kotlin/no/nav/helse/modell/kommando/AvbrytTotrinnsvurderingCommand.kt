package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId

internal class AvbrytTotrinnsvurderingCommand(
    private val identitetsnummer: Identitetsnummer,
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

        val vedtaksperiodeIderForOverstyringer = totrinnsvurdering.overstyringer.map { VedtaksperiodeId(it.vedtaksperiodeId) }

        val erAlleVedtaksperiodeneForkastet =
            vedtaksperiodeIderForOverstyringer
                .mapNotNull { sessionContext.vedtaksperiodeRepository.finn(it) }
                .all { it.forkastet }

        if (erAlleVedtaksperiodeneForkastet) {
            totrinnsvurdering.forkast()
        }
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}
