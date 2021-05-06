package no.nav.helse.modell.kommando

import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.util.*

internal class PersisterVedtaksperiodetypeCommand(
    private val vedtaksperiodeId: UUID,
    private val vedtaksperiodetype: Periodetype,
    private val inntektskilde: Inntektskilde,
    private val vedtakDao: VedtakDao
) :
    Command {
    override fun execute(context: CommandContext): Boolean {
        vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, vedtaksperiodetype, inntektskilde)
        return true
    }
}
