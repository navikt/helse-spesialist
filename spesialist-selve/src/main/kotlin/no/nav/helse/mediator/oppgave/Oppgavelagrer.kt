package no.nav.helse.mediator.oppgave

import no.nav.helse.db.TildelingRepository
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilDatabaseversjon
import no.nav.helse.modell.oppgave.OppgaveDto

class Oppgavelagrer(private val tildelingRepository: TildelingRepository) {
    internal fun lagre(
        oppgaveService: OppgaveService,
        oppgaveDto: OppgaveDto,
    ) {
        oppgaveService.opprett(
            id = oppgaveDto.id,
            vedtaksperiodeId = oppgaveDto.vedtaksperiodeId,
            behandlingId = oppgaveDto.behandlingId,
            utbetalingId = oppgaveDto.utbetalingId,
            egenskaper = oppgaveDto.egenskaper.map { it.tilDatabaseversjon() },
            godkjenningsbehovId = oppgaveDto.godkjenningsbehovId,
            kanAvvises = oppgaveDto.kanAvvises,
        )
        oppdaterTildeling(oppgaveDto)

        val totrinnsvurdering = oppgaveDto.totrinnsvurdering
        if (totrinnsvurdering != null) oppgaveService.lagreTotrinnsvurdering(totrinnsvurdering)
    }

    internal fun oppdater(
        oppgaveService: OppgaveService,
        oppgaveDto: OppgaveDto,
    ) {
        oppgaveService.oppdater(
            oppgaveId = oppgaveDto.id,
            status = status(oppgaveDto.tilstand),
            ferdigstiltAvIdent = oppgaveDto.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgaveDto.ferdigstiltAvOid,
            egenskaper = oppgaveDto.egenskaper.map { it.tilDatabaseversjon() },
        )
        oppdaterTildeling(oppgaveDto)

        val totrinnsvurdering = oppgaveDto.totrinnsvurdering
        if (totrinnsvurdering != null) oppgaveService.lagreTotrinnsvurdering(totrinnsvurdering)
    }

    private fun oppdaterTildeling(oppgaveDto: OppgaveDto) {
        val tildeltTil = oppgaveDto.tildeltTil
        if (tildeltTil != null) {
            tildelingRepository.tildel(oppgaveDto.id, tildeltTil.oid)
        } else {
            tildelingRepository.avmeld(oppgaveDto.id)
        }
    }

    private fun status(tilstand: OppgaveDto.TilstandDto): String {
        return when (tilstand) {
            OppgaveDto.TilstandDto.AvventerSaksbehandler -> "AvventerSaksbehandler"
            OppgaveDto.TilstandDto.AvventerSystem -> "AvventerSystem"
            OppgaveDto.TilstandDto.Ferdigstilt -> "Ferdigstilt"
            OppgaveDto.TilstandDto.Invalidert -> "Invalidert"
        }
    }
}
