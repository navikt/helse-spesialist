package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.toDto
import no.nav.helse.modell.oppgave.EgenskapDto
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.gjenopprett
import no.nav.helse.modell.oppgave.OppgaveDto
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDto

class Oppgavehenter(
    private val oppgaveDao: OppgaveDao,
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val tilgangskontroll: Tilgangskontroll,
) {
    fun oppgave(id: Long): Oppgave {
        val oppgave =
            oppgaveDao.finnOppgave(id)
                ?: throw IllegalStateException("Forventer å finne oppgave med oppgaveId=$id")
        val totrinnsvurdering = totrinnsvurderingDao.hentAktivTotrinnsvurdering(id)

        val dto =
            OppgaveDto(
                id = oppgave.id,
                tilstand = tilstand(oppgave.status),
                vedtaksperiodeId = oppgave.vedtaksperiodeId,
                behandlingId = oppgave.behandlingId,
                utbetalingId = oppgave.utbetalingId,
                godkjenningsbehovId = oppgave.godkjenningsbehovId,
                kanAvvises = oppgave.kanAvvises,
                egenskaper = oppgave.egenskaper.map { it.toDto() },
                totrinnsvurdering =
                    totrinnsvurdering?.let {
                        TotrinnsvurderingDto(
                            vedtaksperiodeId = it.vedtaksperiodeId,
                            erRetur = it.erRetur,
                            saksbehandler =
                                it.saksbehandler?.let {
                                    saksbehandlerDao.finnSaksbehandler(it)?.toDto()
                                },
                            beslutter =
                                it.beslutter?.let {
                                    saksbehandlerDao.finnSaksbehandler(it)?.toDto()
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

    private fun EgenskapForDatabase.toDto(): EgenskapDto =
        when (this) {
            EgenskapForDatabase.SØKNAD -> EgenskapDto.SØKNAD
            EgenskapForDatabase.STIKKPRØVE -> EgenskapDto.STIKKPRØVE
            EgenskapForDatabase.RISK_QA -> EgenskapDto.RISK_QA
            EgenskapForDatabase.REVURDERING -> EgenskapDto.REVURDERING
            EgenskapForDatabase.FORTROLIG_ADRESSE -> EgenskapDto.FORTROLIG_ADRESSE
            EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE -> EgenskapDto.STRENGT_FORTROLIG_ADRESSE
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> EgenskapDto.UTBETALING_TIL_SYKMELDT
            EgenskapForDatabase.DELVIS_REFUSJON -> EgenskapDto.DELVIS_REFUSJON
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapDto.UTBETALING_TIL_ARBEIDSGIVER
            EgenskapForDatabase.INGEN_UTBETALING -> EgenskapDto.INGEN_UTBETALING
            EgenskapForDatabase.EGEN_ANSATT -> EgenskapDto.EGEN_ANSATT
            EgenskapForDatabase.EN_ARBEIDSGIVER -> EgenskapDto.EN_ARBEIDSGIVER
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> EgenskapDto.FLERE_ARBEIDSGIVERE
            EgenskapForDatabase.UTLAND -> EgenskapDto.UTLAND
            EgenskapForDatabase.HASTER -> EgenskapDto.HASTER
            EgenskapForDatabase.BESLUTTER -> EgenskapDto.BESLUTTER
            EgenskapForDatabase.RETUR -> EgenskapDto.RETUR
            EgenskapForDatabase.VERGEMÅL -> EgenskapDto.VERGEMÅL
            EgenskapForDatabase.SPESIALSAK -> EgenskapDto.SPESIALSAK
            EgenskapForDatabase.FORLENGELSE -> EgenskapDto.FORLENGELSE
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> EgenskapDto.FORSTEGANGSBEHANDLING
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> EgenskapDto.INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> EgenskapDto.OVERGANG_FRA_IT
            EgenskapForDatabase.SKJØNNSFASTSETTELSE -> EgenskapDto.SKJØNNSFASTSETTELSE
            EgenskapForDatabase.PÅ_VENT -> EgenskapDto.PÅ_VENT
            EgenskapForDatabase.TILBAKEDATERT -> EgenskapDto.TILBAKEDATERT
            EgenskapForDatabase.GOSYS -> EgenskapDto.GOSYS
            EgenskapForDatabase.MANGLER_IM -> EgenskapDto.MANGLER_IM
            EgenskapForDatabase.MEDLEMSKAP -> EgenskapDto.MEDLEMSKAP
            EgenskapForDatabase.TILKOMMEN -> EgenskapDto.TILKOMMEN
        }
}
