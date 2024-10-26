package no.nav.helse.mediator.oppgave

import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.toDto
import no.nav.helse.mediator.oppgave.OppgaveMapper.toDto
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.gjenopprett
import no.nav.helse.modell.oppgave.OppgaveDto
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDto

class Oppgavehenter(
    private val oppgaveRepository: OppgaveRepository,
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val tilgangskontroll: Tilgangskontroll,
) {
    fun oppgave(id: Long): Oppgave {
        val oppgave =
            oppgaveRepository.finnOppgave(id)
                ?: throw IllegalStateException("Forventer å finne oppgave med oppgaveId=$id")
        val totrinnsvurdering = totrinnsvurderingDao.hentAktivTotrinnsvurdering(id)

        val dto =
            OppgaveDto(
                id = oppgave.id,
                tilstand = tilstand(oppgave.status),
                vedtaksperiodeId = oppgave.vedtaksperiodeId,
                utbetalingId = oppgave.utbetalingId,
                hendelseId = oppgave.hendelseId,
                kanAvvises = oppgave.kanAvvises,
                egenskaper = oppgave.egenskaper.map { it.toDto() },
                totrinnsvurdering =
                    totrinnsvurdering?.let {
                        TotrinnsvurderingDto(
                            vedtaksperiodeId = it.vedtaksperiodeId,
                            erRetur = it.erRetur,
                            saksbehandler =
                                it.saksbehandler?.let {
                                    saksbehandlerRepository.finnSaksbehandler(it)?.toDto()
                                },
                            beslutter =
                                it.beslutter?.let {
                                    saksbehandlerRepository.finnSaksbehandler(it)?.toDto()
                                },
                            utbetalingId = it.utbetalingId,
                            opprettet = it.opprettet,
                            oppdatert = it.oppdatert,
                        )
                    },
                ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
                ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
                tildeltTil = oppgave.tildelt?.toDto(),
            )

        return dto.gjenopprett(tilgangskontroll)
    }

    private fun tilstand(oppgavestatus: String): OppgaveDto.TilstandDto {
        return when (oppgavestatus) {
            "AvventerSaksbehandler" -> OppgaveDto.TilstandDto.AvventerSaksbehandler
            "AvventerSystem" -> OppgaveDto.TilstandDto.AvventerSystem
            "Ferdigstilt" -> OppgaveDto.TilstandDto.Ferdigstilt
            "Invalidert" -> OppgaveDto.TilstandDto.Invalidert
            else -> throw IllegalStateException("Oppgavestatus $oppgavestatus er ikke en gyldig status")
        }
    }
}
