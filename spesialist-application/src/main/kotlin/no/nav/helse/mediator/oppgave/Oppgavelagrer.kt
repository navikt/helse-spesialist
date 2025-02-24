package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Tilstand

class Oppgavelagrer(private val oppgaveDao: OppgaveDao, private val tildelingDao: TildelingDao) {
    fun lagre(oppgave: Oppgave) {
        oppgaveDao.opprettOppgave(
            id = oppgave.id,
            godkjenningsbehovId = oppgave.godkjenningsbehovId,
            egenskaper = oppgave.egenskaper.map { it.tilDatabaseversjon() },
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            behandlingId = oppgave.behandlingId,
            utbetalingId = oppgave.utbetalingId,
            kanAvvises = oppgave.kanAvvises,
        )
        oppdaterTildeling(oppgave)
    }

    fun oppdater(
        oppgaveService: OppgaveService,
        oppgave: Oppgave,
    ) {
        oppgaveService.oppdater(
            oppgaveId = oppgave.id,
            status = status(oppgave.tilstand),
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            egenskaper = oppgave.egenskaper.map { it.tilDatabaseversjon() },
        )
        oppdaterTildeling(oppgave)
    }

    private fun oppdaterTildeling(oppgave: Oppgave) {
        val tildeltTil = oppgave.tildeltTil
        if (tildeltTil != null) {
            tildelingDao.tildel(oppgave.id, tildeltTil.oid)
        } else {
            tildelingDao.avmeld(oppgave.id)
        }
    }

    private fun status(tilstand: Tilstand): String {
        return when (tilstand) {
            Oppgave.AvventerSaksbehandler -> "AvventerSaksbehandler"
            Oppgave.AvventerSystem -> "AvventerSystem"
            Oppgave.Ferdigstilt -> "Ferdigstilt"
            Oppgave.Invalidert -> "Invalidert"
        }
    }

    private fun Egenskap.tilDatabaseversjon() =
        when (this) {
            Egenskap.RISK_QA -> EgenskapForDatabase.RISK_QA
            Egenskap.FORTROLIG_ADRESSE -> EgenskapForDatabase.FORTROLIG_ADRESSE
            Egenskap.STRENGT_FORTROLIG_ADRESSE -> EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE
            Egenskap.EGEN_ANSATT -> EgenskapForDatabase.EGEN_ANSATT
            Egenskap.BESLUTTER -> EgenskapForDatabase.BESLUTTER
            Egenskap.SPESIALSAK -> EgenskapForDatabase.SPESIALSAK
            Egenskap.REVURDERING -> EgenskapForDatabase.REVURDERING
            Egenskap.SØKNAD -> EgenskapForDatabase.SØKNAD
            Egenskap.STIKKPRØVE -> EgenskapForDatabase.STIKKPRØVE
            Egenskap.UTBETALING_TIL_SYKMELDT -> EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
            Egenskap.DELVIS_REFUSJON -> EgenskapForDatabase.DELVIS_REFUSJON
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER
            Egenskap.INGEN_UTBETALING -> EgenskapForDatabase.INGEN_UTBETALING
            Egenskap.EN_ARBEIDSGIVER -> EgenskapForDatabase.EN_ARBEIDSGIVER
            Egenskap.FLERE_ARBEIDSGIVERE -> EgenskapForDatabase.FLERE_ARBEIDSGIVERE
            Egenskap.FORLENGELSE -> EgenskapForDatabase.FORLENGELSE
            Egenskap.FORSTEGANGSBEHANDLING -> EgenskapForDatabase.FORSTEGANGSBEHANDLING
            Egenskap.INFOTRYGDFORLENGELSE -> EgenskapForDatabase.INFOTRYGDFORLENGELSE
            Egenskap.OVERGANG_FRA_IT -> EgenskapForDatabase.OVERGANG_FRA_IT
            Egenskap.UTLAND -> EgenskapForDatabase.UTLAND
            Egenskap.HASTER -> EgenskapForDatabase.HASTER
            Egenskap.RETUR -> EgenskapForDatabase.RETUR
            Egenskap.SKJØNNSFASTSETTELSE -> EgenskapForDatabase.SKJØNNSFASTSETTELSE
            Egenskap.PÅ_VENT -> EgenskapForDatabase.PÅ_VENT
            Egenskap.TILBAKEDATERT -> EgenskapForDatabase.TILBAKEDATERT
            Egenskap.GOSYS -> EgenskapForDatabase.GOSYS
            Egenskap.MANGLER_IM -> EgenskapForDatabase.MANGLER_IM
            Egenskap.MEDLEMSKAP -> EgenskapForDatabase.MEDLEMSKAP
            Egenskap.VERGEMÅL -> EgenskapForDatabase.VERGEMÅL
            Egenskap.TILKOMMEN -> EgenskapForDatabase.TILKOMMEN
        }
}
