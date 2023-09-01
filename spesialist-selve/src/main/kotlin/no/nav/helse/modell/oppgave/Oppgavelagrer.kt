package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype

class Oppgavelagrer : OppgaveVisitor {
    private lateinit var oppgaveForLagring: OppgaveForLagring

    private class OppgaveForLagring(
        val id: Long,
        val type: Oppgavetype,
        var status: Oppgavestatus,
        val vedtaksperiodeId: UUID,
        val utbetalingId: UUID,
        var ferdigstiltAvIdent: String?,
        var ferdigstiltAvOid: UUID?,
        val egenskaper: List<Oppgavetype>,
    )

    internal fun lagre(oppgaveMediator: OppgaveMediator, hendelseId: UUID, contextId: UUID) {
        oppgaveMediator.opprett(
            oppgaveForLagring.id,
            contextId,
            oppgaveForLagring.vedtaksperiodeId,
            oppgaveForLagring.utbetalingId,
            oppgaveForLagring.type,
            hendelseId
        )
    }

    internal fun lagre(oppgaveMediator: OppgaveMediator) {
        oppgaveMediator.oppdater(
            oppgaveId = oppgaveForLagring.id,
            status = oppgaveForLagring.status,
            ferdigstiltAvIdent = oppgaveForLagring.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgaveForLagring.ferdigstiltAvOid
        )
    }

    override fun visitOppgave(
        id: Long,
        type: Oppgavetype,
        status: Oppgavestatus,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Oppgavetype>
    ) {
        oppgaveForLagring = OppgaveForLagring(
            id, type, status, vedtaksperiodeId, utbetalingId, ferdigstiltAvIdent, ferdigstiltAvOid, egenskaper
        )
    }
}