package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import java.util.UUID

class UnimplementedVedtakBegrunnelseDao : VedtakBegrunnelseDao {
    override fun invaliderVedtakBegrunnelse(oppgaveId: Long): Int {
        TODO("Not yet implemented")
    }

    override fun finnVedtakBegrunnelse(
        vedtaksperiodeId: UUID,
        generasjonId: Long
    ): VedtakBegrunnelse? {
        TODO("Not yet implemented")
    }

    override fun finnVedtakBegrunnelse(oppgaveId: Long): VedtakBegrunnelseFraDatabase? {
        TODO("Not yet implemented")
    }

    override fun finnAlleVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID
    ): List<VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase> {
        TODO("Not yet implemented")
    }

    override fun lagreVedtakBegrunnelse(
        oppgaveId: Long,
        vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        saksbehandlerOid: UUID
    ): Int {
        TODO("Not yet implemented")
    }
}
