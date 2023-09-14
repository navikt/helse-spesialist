package no.nav.helse.mediator.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype

class Oppgavelagrer(private val tildelingDao: TildelingDao) : OppgaveVisitor {
    private lateinit var oppgaveForLagring: OppgaveFraDatabase
    private var totrinnsvurderingForLagring: TotrinnsvurderingFraDatabase? = null

    internal fun lagre(oppgaveMediator: OppgaveMediator, contextId: UUID) {
        val oppgave = oppgaveForLagring
        oppgaveMediator.opprett(
            id = oppgave.id,
            contextId = contextId,
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            navn = enumValueOf(oppgave.type),
            hendelseId = oppgave.hendelseId
        )
        if (oppgave.tildelt != null) tildelingDao.tildel(oppgave.id, oppgave.tildelt.oid, oppgave.påVent)
        else tildelingDao.avmeld(oppgave.id)

        val totrinnsvurdering = totrinnsvurderingForLagring
        if (totrinnsvurdering != null) oppgaveMediator.lagreTotrinnsvurdering(totrinnsvurdering)
    }

    internal fun oppdater(oppgaveMediator: OppgaveMediator) {
        val oppgave = oppgaveForLagring
        oppgaveMediator.oppdater(
            oppgaveId = oppgave.id,
            status = enumValueOf(oppgave.status),
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid
        )
        if (oppgave.tildelt != null) tildelingDao.tildel(oppgave.id, oppgave.tildelt.oid, oppgave.påVent)
        else tildelingDao.avmeld(oppgave.id)

        val totrinnsvurdering = totrinnsvurderingForLagring
        if (totrinnsvurdering != null) oppgaveMediator.lagreTotrinnsvurdering(totrinnsvurdering)
    }

    override fun visitOppgave(
        id: Long,
        type: Oppgavetype,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Oppgavetype>,
        tildelt: Saksbehandler?,
        påVent: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {
        val status = status(tilstand)
        oppgaveForLagring = OppgaveFraDatabase(
            id = id,
            type = type.toString(),
            status = status.toString(),
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            hendelseId = hendelseId,
            ferdigstiltAvIdent = ferdigstiltAvIdent,
            ferdigstiltAvOid = ferdigstiltAvOid,
            tildelt = tildelt?.toDto()?.let {
               SaksbehandlerFraDatabase(it.epost, it.oid, it.navn, it.ident)
            },
            påVent = påVent
        )
    }

    override fun visitTotrinnsvurdering(
        vedtaksperiodeId: UUID,
        erRetur: Boolean,
        saksbehandler: Saksbehandler?,
        beslutter: Saksbehandler?,
        utbetalingId: UUID?,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime?
    ) {
        totrinnsvurderingForLagring = TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = erRetur,
            saksbehandler = saksbehandler?.oid(),
            beslutter = beslutter?.oid(),
            utbetalingId = utbetalingId,
            opprettet = opprettet,
            oppdatert = oppdatert
        )
    }

    private fun status(tilstand: Oppgave.Tilstand): Oppgavestatus {
        return when (tilstand) {
            Oppgave.AvventerSaksbehandler -> Oppgavestatus.AvventerSaksbehandler
            Oppgave.AvventerSystem -> Oppgavestatus.AvventerSystem
            Oppgave.Ferdigstilt -> Oppgavestatus.Ferdigstilt
            Oppgave.Invalidert -> Oppgavestatus.Invalidert
        }
    }
}