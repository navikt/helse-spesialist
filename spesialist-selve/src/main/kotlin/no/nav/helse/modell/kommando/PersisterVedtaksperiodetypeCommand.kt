package no.nav.helse.modell.kommando

import no.nav.helse.db.VedtakRepository
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.util.UUID

internal class PersisterVedtaksperiodetypeCommand(
    private val vedtaksperiodeId: UUID,
    private val vedtaksperiodetype: Periodetype,
    private val inntektskilde: Inntektskilde,
    private val vedtakRepository: VedtakRepository,
) :
    Command {
    override fun execute(context: CommandContext): Boolean {
        vedtakRepository.leggTilVedtaksperiodetype(vedtaksperiodeId, vedtaksperiodetype, inntektskilde)
        return true
    }
}
