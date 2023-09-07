package no.nav.helse.mediator.oppgave

import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype

class Oppgavehenter(
    private val oppgaveRepository: OppgaveRepository,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository
) {
    fun oppgave(id: Long): Oppgave {
        val oppgave = oppgaveRepository.finnOppgave(id)
            ?: throw IllegalStateException("Forventer å finne oppgave med oppgaveId=$id")
        val totrinnsvurdering = totrinnsvurderingRepository.hentAktivTotrinnsvurdering(id)

        return Oppgave(
            id = oppgave.id,
            type = enumValueOf<Oppgavetype>(oppgave.type),
            tilstand = tilstand(enumValueOf<Oppgavestatus>(oppgave.status)),
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            tildelt = oppgave.tildelt?.let {
                Saksbehandler(it.epostadresse, it.oid, it.navn, it.ident)
            },
            påVent = oppgave.påVent,
            totrinnsvurdering = totrinnsvurdering?.let {
                Totrinnsvurdering(
                    vedtaksperiodeId = it.vedtaksperiodeId,
                    erRetur = it.erRetur,
                    saksbehandler = it.saksbehandler?.let(saksbehandlerRepository::finnSaksbehandler)?.toSaksbehandler(),
                    beslutter = it.beslutter?.let(saksbehandlerRepository::finnSaksbehandler)?.toSaksbehandler(),
                    utbetalingIdRef = it.utbetalingIdRef,
                    opprettet = it.opprettet,
                    oppdatert = it.oppdatert
                )
            }
        )
    }

    private fun tilstand(oppgavestatus: Oppgavestatus): Oppgave.Tilstand {
        return when (oppgavestatus) {
            Oppgavestatus.AvventerSaksbehandler -> Oppgave.AvventerSaksbehandler
            Oppgavestatus.AvventerSystem -> Oppgave.AvventerSystem
            Oppgavestatus.Ferdigstilt -> Oppgave.Ferdigstilt
            Oppgavestatus.Invalidert -> Oppgave.Invalidert
        }
    }

    private fun SaksbehandlerFraDatabase.toSaksbehandler() = Saksbehandler(epostadresse, oid, navn, ident)
}