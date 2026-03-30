package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.application.Outbox
import java.util.UUID

internal class PersisterVedtaksperiodetypeCommand(
    private val vedtaksperiodeId: UUID,
    private val vedtaksperiodetype: Periodetype,
    private val inntektskilde: Inntektskilde,
    private val vedtakDao: VedtakDao,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, vedtaksperiodetype, inntektskilde)
        return true
    }
}
