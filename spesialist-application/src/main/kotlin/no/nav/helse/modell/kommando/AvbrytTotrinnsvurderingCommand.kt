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
        val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPersonOrNull(identitetsnummer.value) ?: return true

        val vedtaksperiodeIderForOverstyringer = totrinnsvurdering.overstyringer.map { VedtaksperiodeId(it.vedtaksperiodeId) }

        val relaterteVedtaksperioder = sessionContext.vedtaksperiodeRepository.finn(vedtaksperiodeIderForOverstyringer)
        val erAlleVedtaksperiodeneForkastet = relaterteVedtaksperioder.all { it.forkastet }

        if (erAlleVedtaksperiodeneForkastet) {
            loggInfo(
                "forkaster totrinnsvurderingen fordi alle relaterte vedtaksperioder er forkastet",
                "fødselsnummer" to identitetsnummer.value,
            )
            totrinnsvurdering.forkast()
        } else {
            val melding =
                if (totrinnsvurdering.beslutter == null) {
                    "Totrinnsvurderingen har ikke beslutter"
                } else {
                    "Beholder beslutter"
                }

            loggInfo(
                "Forkaster ikke totrinnsvurderingen, fordi den henger sammen med vedtaksperioder som ikke er forkastet: ${relaterteVedtaksperioder.filterNot { it.forkastet }}. $melding",
                "fødselsnummer" to identitetsnummer.value,
            )
        }
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}
