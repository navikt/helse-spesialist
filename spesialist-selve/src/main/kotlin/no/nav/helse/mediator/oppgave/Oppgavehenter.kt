package no.nav.helse.mediator.oppgave

import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.FULLMAKT
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.RETUR
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
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
            egenskap = egenskap(oppgave.egenskap),
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
            egenskaper = oppgave.egenskaper.map { egenskap(it) }
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

    private fun egenskap(egenskap: String): Egenskap {
        return when (egenskap) {
            "RISK_QA" -> RISK_QA
            "FORTROLIG_ADRESSE" -> FORTROLIG_ADRESSE
            "EGEN_ANSATT" -> EGEN_ANSATT
            "REVURDERING" -> REVURDERING
            "SØKNAD" -> SØKNAD
            "STIKKPRØVE" -> STIKKPRØVE
            "UTBETALING_TIL_SYKMELDT" -> UTBETALING_TIL_SYKMELDT
            "DELVIS_REFUSJON" -> DELVIS_REFUSJON
            "UTBETALING_TIL_ARBEIDSGIVER" -> UTBETALING_TIL_ARBEIDSGIVER
            "INGEN_UTBETALING" -> INGEN_UTBETALING
            "EN_ARBEIDSGIVER" -> EN_ARBEIDSGIVER
            "FLERE_ARBEIDSGIVERE" -> FLERE_ARBEIDSGIVERE
            "UTLAND" -> UTLAND
            "HASTER" -> HASTER
            "RETUR" -> RETUR
            "BESLUTTER" -> BESLUTTER
            "FULLMAKT" -> FULLMAKT
            "VERGEMÅL" -> VERGEMÅL
            "SPESIALSAK" -> SPESIALSAK
            else -> throw IllegalStateException("Egenskap $egenskap er ikke en gyldig egenskap")
        }
    }

    private fun SaksbehandlerFraDatabase.toSaksbehandler() = Saksbehandler(epostadresse, oid, navn, ident, tilgangskontroll)
}