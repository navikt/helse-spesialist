package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.FULLMAKT
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.OVERGANG_FRA_IT
import no.nav.helse.modell.oppgave.Egenskap.RETUR
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.oppgave.Egenskap.VERGEMÅL
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
            egenskap = enumValueOf<EgenskapForDatabase>(oppgave.egenskap).tilEgenskap(),
            tilstand = tilstand(oppgave.status),
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            hendelseId = oppgave.hendelseId,
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            tildelt = oppgave.tildelt?.let {
                Saksbehandler(it.epostadresse, it.oid, it.navn, it.ident, tilgangskontroll)
            },
            påVent = oppgave.påVent,
            kanAvvises = oppgave.kanAvvises,
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
            egenskaper = oppgave.egenskaper.map { it.tilEgenskap() }
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

    private fun EgenskapForDatabase.tilEgenskap(): Egenskap {
        return when (this) {
            EgenskapForDatabase.RISK_QA -> RISK_QA
            EgenskapForDatabase.FORTROLIG_ADRESSE -> FORTROLIG_ADRESSE
            EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE -> STRENGT_FORTROLIG_ADRESSE
            EgenskapForDatabase.EGEN_ANSATT -> EGEN_ANSATT
            EgenskapForDatabase.REVURDERING -> REVURDERING
            EgenskapForDatabase.SØKNAD -> SØKNAD
            EgenskapForDatabase.STIKKPRØVE -> STIKKPRØVE
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> UTBETALING_TIL_SYKMELDT
            EgenskapForDatabase.DELVIS_REFUSJON -> DELVIS_REFUSJON
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> UTBETALING_TIL_ARBEIDSGIVER
            EgenskapForDatabase.INGEN_UTBETALING -> INGEN_UTBETALING
            EgenskapForDatabase.EN_ARBEIDSGIVER -> EN_ARBEIDSGIVER
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> FLERE_ARBEIDSGIVERE
            EgenskapForDatabase.FORLENGELSE -> FORLENGELSE
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> FORSTEGANGSBEHANDLING
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> OVERGANG_FRA_IT
            EgenskapForDatabase.UTLAND -> UTLAND
            EgenskapForDatabase.HASTER -> HASTER
            EgenskapForDatabase.RETUR -> RETUR
            EgenskapForDatabase.BESLUTTER -> BESLUTTER
            EgenskapForDatabase.FULLMAKT -> FULLMAKT
            EgenskapForDatabase.VERGEMÅL -> VERGEMÅL
            EgenskapForDatabase.SPESIALSAK -> SPESIALSAK
        }
    }

    private fun SaksbehandlerFraDatabase.toSaksbehandler() = Saksbehandler(epostadresse, oid, navn, ident, tilgangskontroll)
}