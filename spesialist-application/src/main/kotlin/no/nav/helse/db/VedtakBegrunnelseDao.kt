package no.nav.helse.db

import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import java.util.UUID

interface VedtakBegrunnelseDao {
    fun invaliderVedtakBegrunnelse(oppgaveId: Long): Int

    fun finnVedtakBegrunnelse(
        vedtaksperiodeId: UUID,
        generasjonId: Long,
    ): VedtakBegrunnelse?

    fun finnVedtakBegrunnelse(oppgaveId: Long): VedtakBegrunnelseFraDatabase?

    fun finnAlleVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase>

    fun lagreVedtakBegrunnelse(
        oppgaveId: Long,
        vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        saksbehandlerOid: UUID,
    ): Int
}
