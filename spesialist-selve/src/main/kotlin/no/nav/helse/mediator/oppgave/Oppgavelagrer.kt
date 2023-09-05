package no.nav.helse.mediator.oppgave

import java.util.UUID
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.spesialist.api.modell.Saksbehandler
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
        val tildelt: Saksbehandler?,
        val påVent: Boolean
    )

    internal fun lagre(oppgaveMediator: OppgaveMediator, hendelseId: UUID, contextId: UUID) {
        val oppgave = oppgaveForLagring
        oppgaveMediator.opprett(
            id = oppgave.id,
            contextId = contextId,
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            navn = oppgave.type,
            hendelseId = hendelseId
        )
        if (oppgave.tildelt != null) {
            oppgaveMediator.tildel(oppgave.id, oppgave.tildelt.oid(), oppgave.påVent)
        }
    }

    internal fun oppdater(oppgaveMediator: OppgaveMediator) {
        val oppgave = oppgaveForLagring
        oppgaveMediator.oppdater(
            oppgaveId = oppgave.id,
            status = oppgave.status,
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid
        )
        if (oppgave.tildelt != null) {
            oppgaveMediator.tildel(oppgave.id, oppgave.tildelt.oid(), oppgave.påVent)
        }
    }

    override fun visitOppgave(
        id: Long,
        type: Oppgavetype,
        status: Oppgavestatus,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Oppgavetype>,
        tildelt: Saksbehandler?,
        påVent: Boolean
    ) {
        oppgaveForLagring = OppgaveForLagring(
            id, type, status, vedtaksperiodeId, utbetalingId, ferdigstiltAvIdent, ferdigstiltAvOid, egenskaper, tildelt, påVent
        )
    }
}