package no.nav.helse.mediator.oppgave

import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilDatabaseversjon
import no.nav.helse.modell.oppgave.OppgaveDto
import java.util.UUID

class Oppgavelagrer(private val tildelingDao: TildelingDao) {
    internal fun lagre(
        oppgaveService: OppgaveService,
        oppgaveDto: OppgaveDto,
        contextId: UUID,
    ) {
        oppgaveService.opprett(
            id = oppgaveDto.id,
            contextId = contextId,
            vedtaksperiodeId = oppgaveDto.vedtaksperiodeId,
            utbetalingId = oppgaveDto.utbetalingId,
            egenskaper = oppgaveDto.egenskaper.map { it.tilDatabaseversjon() },
            hendelseId = oppgaveDto.hendelseId,
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
            tildelingDao.tildel(oppgaveDto.id, tildeltTil.oid)
        } else {
            tildelingDao.avmeld(oppgaveDto.id)
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
