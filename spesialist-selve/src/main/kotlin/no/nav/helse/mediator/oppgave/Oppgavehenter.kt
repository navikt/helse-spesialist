package no.nav.helse.mediator.oppgave

import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilModellversjon
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

class Oppgavehenter(
    private val oppgaveRepository: OppgaveRepository,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val tilgangskontroll: Tilgangskontroll,
) {
    fun oppgave(id: Long): Oppgave {
        val oppgave = oppgaveRepository.finnOppgave(id)
            ?: throw IllegalStateException("Forventer å finne oppgave med oppgaveId=$id")
        val totrinnsvurdering = totrinnsvurderingRepository.hentAktivTotrinnsvurdering(id)

        return Oppgave(
            id = oppgave.id,
            tilstand = tilstand(oppgave.status),
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            hendelseId = oppgave.hendelseId,
            kanAvvises = oppgave.kanAvvises,
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            tildelt = oppgave.tildelt?.let {
                Saksbehandler(it.epostadresse, it.oid, it.navn, it.ident, tilgangskontroll)
            },
            totrinnsvurdering = totrinnsvurdering?.let {
                Totrinnsvurdering(
                    vedtaksperiodeId = it.vedtaksperiodeId,
                    erRetur = it.erRetur,
                    saksbehandler = it.saksbehandler?.let(saksbehandlerRepository::finnSaksbehandler)?.toSaksbehandler(),
                    beslutter = it.beslutter?.let(saksbehandlerRepository::finnSaksbehandler)?.toSaksbehandler(),
                    utbetalingId = it.utbetalingId,
                    opprettet = it.opprettet,
                    oppdatert = it.oppdatert
                )
            },
            egenskaper = oppgave.egenskaper.map { it.tilModellversjon() }
        )
    }

    private fun tilstand(oppgavestatus: String): Oppgave.Tilstand {
        return when (oppgavestatus) {
            "AvventerSaksbehandler" -> Oppgave.AvventerSaksbehandler
            "AvventerSystem" -> Oppgave.AvventerSystem
            "Ferdigstilt" -> Oppgave.Ferdigstilt
            "Invalidert" -> Oppgave.Invalidert
            else -> throw IllegalStateException("Oppgavestatus $oppgavestatus er ikke en gyldig status")
        }
    }

    private fun SaksbehandlerFraDatabase.toSaksbehandler() = Saksbehandler(epostadresse, oid, navn, ident, tilgangskontroll)
}