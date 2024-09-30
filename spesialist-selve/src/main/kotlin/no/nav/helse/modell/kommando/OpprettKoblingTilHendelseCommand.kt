package no.nav.helse.modell.kommando

import no.nav.helse.db.VedtakRepository
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData

internal class OpprettKoblingTilHendelseCommand(
    commandData: GodkjenningsbehovData,
    private val vedtakRepository: VedtakRepository,
) : Command {
    private val meldingId = commandData.id

    private val vedtaksperiodeId = commandData.vedtaksperiodeId

    override fun execute(context: CommandContext): Boolean {
        vedtakRepository.opprettKobling(vedtaksperiodeId, meldingId)
        return true
    }

    override fun undo(context: CommandContext) {
        vedtakRepository.fjernKobling(vedtaksperiodeId, meldingId)
    }
}
