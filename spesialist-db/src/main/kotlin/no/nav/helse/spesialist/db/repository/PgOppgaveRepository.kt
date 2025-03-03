package no.nav.helse.spesialist.db.repository

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

class PgOppgaveRepository(private val oppgaveDao: OppgaveDao, private val tildelingDao: TildelingDao) :
    OppgaveRepository {
    override fun lagre(oppgave: Oppgave) {
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

    override fun oppdater(oppgave: Oppgave) {
        oppgaveDao.updateOppgave(
            oppgaveId = oppgave.id,
            oppgavestatus = status(oppgave.tilstand),
            ferdigstiltAv = oppgave.ferdigstiltAvIdent,
            oid = oppgave.ferdigstiltAvOid,
            egenskaper = oppgave.egenskaper.map { it.tilDatabaseversjon() },
        )
        oppdaterTildeling(oppgave)
    }

    override fun oppgave(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave {
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
                    LegacySaksbehandler(
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
            "AvventerSaksbehandler" -> Oppgave.AvventerSaksbehandler
            "AvventerSystem" -> Oppgave.AvventerSystem
            "Ferdigstilt" -> Oppgave.Ferdigstilt
            "Invalidert" -> Oppgave.Invalidert
            else -> throw IllegalStateException("Oppgavestatus $oppgavestatus er ikke en gyldig status")
        }
    }

    private fun oppdaterTildeling(oppgave: Oppgave) {
        val tildeltTil = oppgave.tildeltTil
        if (tildeltTil != null) {
            tildelingDao.tildel(oppgave.id, tildeltTil.oid)
        } else {
            tildelingDao.avmeld(oppgave.id)
        }
    }

    private fun status(tilstand: Oppgave.Tilstand): String {
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
