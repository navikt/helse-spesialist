package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import java.time.LocalDateTime
import java.util.UUID

class InMemoryVedtakBegrunnelseDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val saksbehandlerRepository: InMemorySaksbehandlerRepository,
) : VedtakBegrunnelseDao {
    private data class LagretBegrunnelse(
        val oppgaveId: Long,
        val vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        val saksbehandlerOid: UUID,
        val opprettet: LocalDateTime,
        val invalidert: Boolean,
    )

    private val begrunnelser = mutableListOf<LagretBegrunnelse>()

    override fun lagreVedtakBegrunnelse(
        oppgaveId: Long,
        vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        saksbehandlerOid: UUID,
    ): Int {
        begrunnelser.add(
            LagretBegrunnelse(
                oppgaveId = oppgaveId,
                vedtakBegrunnelse = vedtakBegrunnelse,
                saksbehandlerOid = saksbehandlerOid,
                opprettet = LocalDateTime.now(),
                invalidert = false,
            ),
        )
        return 1
    }

    override fun invaliderVedtakBegrunnelse(oppgaveId: Long): Int {
        val antall = begrunnelser.count { it.oppgaveId == oppgaveId && !it.invalidert }
        begrunnelser.replaceAll { if (it.oppgaveId == oppgaveId) it.copy(invalidert = true) else it }
        return antall
    }

    override fun finnVedtakBegrunnelse(oppgaveId: Long): VedtakBegrunnelseFraDatabase? = begrunnelser.lastOrNull { it.oppgaveId == oppgaveId && !it.invalidert }?.vedtakBegrunnelse

    override fun finnAlleVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase> =
        begrunnelser
            .filter { lagret ->
                val oppgave = oppgaveRepository.finn(OppgaveId(lagret.oppgaveId)) ?: return@filter false
                if (oppgave.vedtaksperiodeId.value != vedtaksperiodeId) return@filter false
                val behandling = behandlingRepository.finn(oppgave.behandlingId) ?: return@filter false
                behandling.utbetalingId?.value == utbetalingId
            }.sortedByDescending { it.opprettet }
            .map { lagret ->
                VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase(
                    type = lagret.vedtakBegrunnelse.type,
                    begrunnelse = lagret.vedtakBegrunnelse.tekst,
                    opprettet = lagret.opprettet,
                    saksbehandlerIdent =
                        saksbehandlerRepository.finn(SaksbehandlerOid(lagret.saksbehandlerOid))?.ident?.value
                            ?: lagret.saksbehandlerOid.toString(),
                    invalidert = lagret.invalidert,
                )
            }
}
