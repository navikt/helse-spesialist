package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.AvventerSaksbehandler
import no.nav.helse.modell.oppgave.Oppgave.AvventerSystem
import no.nav.helse.modell.oppgave.Oppgave.Ferdigstilt
import no.nav.helse.modell.oppgave.Oppgave.Invalidert
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

class Oppgavehenter(
    private val oppgaveDao: OppgaveDao,
    private val tilgangskontroll: Tilgangskontroll,
) {
    fun oppgave(id: Long): Oppgave {
        val oppgave =
            oppgaveDao.finnOppgave(id)
                ?: throw IllegalStateException("Forventer å finne oppgave med oppgaveId=$id")

        return Oppgave.fraLagring(
            id = oppgave.id,
            tilstand = tilstand(oppgave.status),
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            behandlingId = oppgave.behandlingId,
            utbetalingId = oppgave.utbetalingId,
            godkjenningsbehovId = oppgave.godkjenningsbehovId,
            kanAvvises = oppgave.kanAvvises,
            egenskaper = oppgave.egenskaper.map { it.toEgenskap() }.toMutableSet(),
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            tildeltTil =
                oppgave.tildelt?.let {
                    Saksbehandler(
                        epostadresse = it.epostadresse,
                        oid = it.oid,
                        navn = it.navn,
                        ident = it.ident,
                        tilgangskontroll = tilgangskontroll,
                    )
                },
        )
    }

    private fun EgenskapForDatabase.toEgenskap(): Egenskap =
        when (this) {
            EgenskapForDatabase.RISK_QA -> Egenskap.RISK_QA
            EgenskapForDatabase.FORTROLIG_ADRESSE -> Egenskap.FORTROLIG_ADRESSE
            EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE -> Egenskap.STRENGT_FORTROLIG_ADRESSE
            EgenskapForDatabase.EGEN_ANSATT -> Egenskap.EGEN_ANSATT
            EgenskapForDatabase.BESLUTTER -> Egenskap.BESLUTTER
            EgenskapForDatabase.SPESIALSAK -> Egenskap.SPESIALSAK
            EgenskapForDatabase.REVURDERING -> Egenskap.REVURDERING
            EgenskapForDatabase.SØKNAD -> Egenskap.SØKNAD
            EgenskapForDatabase.STIKKPRØVE -> Egenskap.STIKKPRØVE
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
            EgenskapForDatabase.DELVIS_REFUSJON -> Egenskap.DELVIS_REFUSJON
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
            EgenskapForDatabase.INGEN_UTBETALING -> Egenskap.INGEN_UTBETALING
            EgenskapForDatabase.HASTER -> Egenskap.HASTER
            EgenskapForDatabase.RETUR -> Egenskap.RETUR
            EgenskapForDatabase.VERGEMÅL -> Egenskap.VERGEMÅL
            EgenskapForDatabase.EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
            EgenskapForDatabase.UTLAND -> Egenskap.UTLAND
            EgenskapForDatabase.FORLENGELSE -> Egenskap.FORLENGELSE
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
            EgenskapForDatabase.SKJØNNSFASTSETTELSE -> Egenskap.SKJØNNSFASTSETTELSE
            EgenskapForDatabase.PÅ_VENT -> Egenskap.PÅ_VENT
            EgenskapForDatabase.TILBAKEDATERT -> Egenskap.TILBAKEDATERT
            EgenskapForDatabase.GOSYS -> Egenskap.GOSYS
            EgenskapForDatabase.MANGLER_IM -> Egenskap.MANGLER_IM
            EgenskapForDatabase.MEDLEMSKAP -> Egenskap.MEDLEMSKAP
            EgenskapForDatabase.TILKOMMEN -> Egenskap.TILKOMMEN
        }

    private fun tilstand(oppgavestatus: String): Oppgave.Tilstand {
        return when (oppgavestatus) {
            "AvventerSaksbehandler" -> AvventerSaksbehandler
            "AvventerSystem" -> AvventerSystem
            "Ferdigstilt" -> Ferdigstilt
            "Invalidert" -> Invalidert
            else -> throw IllegalStateException("Oppgavestatus $oppgavestatus er ikke en gyldig status")
        }
    }
}
