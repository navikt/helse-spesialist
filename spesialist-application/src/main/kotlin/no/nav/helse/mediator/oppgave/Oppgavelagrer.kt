package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.modell.oppgave.EgenskapDto
import no.nav.helse.modell.oppgave.OppgaveDto

class Oppgavelagrer(private val tildelingDao: TildelingDao) {
    fun lagre(
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

    fun oppdater(
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

    private fun EgenskapDto.tilDatabaseversjon() =
        when (this) {
            EgenskapDto.RISK_QA -> EgenskapForDatabase.RISK_QA
            EgenskapDto.FORTROLIG_ADRESSE -> EgenskapForDatabase.FORTROLIG_ADRESSE
            EgenskapDto.STRENGT_FORTROLIG_ADRESSE -> EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE
            EgenskapDto.EGEN_ANSATT -> EgenskapForDatabase.EGEN_ANSATT
            EgenskapDto.BESLUTTER -> EgenskapForDatabase.BESLUTTER
            EgenskapDto.SPESIALSAK -> EgenskapForDatabase.SPESIALSAK
            EgenskapDto.REVURDERING -> EgenskapForDatabase.REVURDERING
            EgenskapDto.SØKNAD -> EgenskapForDatabase.SØKNAD
            EgenskapDto.STIKKPRØVE -> EgenskapForDatabase.STIKKPRØVE
            EgenskapDto.UTBETALING_TIL_SYKMELDT -> EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
            EgenskapDto.DELVIS_REFUSJON -> EgenskapForDatabase.DELVIS_REFUSJON
            EgenskapDto.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER
            EgenskapDto.INGEN_UTBETALING -> EgenskapForDatabase.INGEN_UTBETALING
            EgenskapDto.EN_ARBEIDSGIVER -> EgenskapForDatabase.EN_ARBEIDSGIVER
            EgenskapDto.FLERE_ARBEIDSGIVERE -> EgenskapForDatabase.FLERE_ARBEIDSGIVERE
            EgenskapDto.FORLENGELSE -> EgenskapForDatabase.FORLENGELSE
            EgenskapDto.FORSTEGANGSBEHANDLING -> EgenskapForDatabase.FORSTEGANGSBEHANDLING
            EgenskapDto.INFOTRYGDFORLENGELSE -> EgenskapForDatabase.INFOTRYGDFORLENGELSE
            EgenskapDto.OVERGANG_FRA_IT -> EgenskapForDatabase.OVERGANG_FRA_IT
            EgenskapDto.UTLAND -> EgenskapForDatabase.UTLAND
            EgenskapDto.HASTER -> EgenskapForDatabase.HASTER
            EgenskapDto.RETUR -> EgenskapForDatabase.RETUR
            EgenskapDto.SKJØNNSFASTSETTELSE -> EgenskapForDatabase.SKJØNNSFASTSETTELSE
            EgenskapDto.PÅ_VENT -> EgenskapForDatabase.PÅ_VENT
            EgenskapDto.TILBAKEDATERT -> EgenskapForDatabase.TILBAKEDATERT
            EgenskapDto.GOSYS -> EgenskapForDatabase.GOSYS
            EgenskapDto.MANGLER_IM -> EgenskapForDatabase.MANGLER_IM
            EgenskapDto.MEDLEMSKAP -> EgenskapForDatabase.MEDLEMSKAP
            EgenskapDto.VERGEMÅL -> EgenskapForDatabase.VERGEMÅL
            EgenskapDto.TILKOMMEN -> EgenskapForDatabase.TILKOMMEN
        }
}
