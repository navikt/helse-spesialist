package no.nav.helse.mediator.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
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
import no.nav.helse.modell.oppgave.Egenskap.VERGEMÅL
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerVisitor
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

class Oppgavelagrer(private val tildelingDao: TildelingDao) : OppgaveVisitor {
    private lateinit var oppgaveForLagring: OppgaveFraDatabase
    private var totrinnsvurderingForLagring: TotrinnsvurderingFraDatabase? = null

    internal fun lagre(oppgaveMediator: OppgaveMediator, contextId: UUID) {
        val oppgave = oppgaveForLagring
        oppgaveMediator.opprett(
            id = oppgave.id,
            contextId = contextId,
            vedtaksperiodeId = oppgave.vedtaksperiodeId,
            utbetalingId = oppgave.utbetalingId,
            egenskap = oppgave.egenskap,
            egenskaper = oppgave.egenskaper,
            hendelseId = oppgave.hendelseId
        )
        if (oppgave.tildelt != null) tildelingDao.tildel(oppgave.id, oppgave.tildelt.oid, oppgave.påVent)
        else tildelingDao.avmeld(oppgave.id)

        val totrinnsvurdering = totrinnsvurderingForLagring
        if (totrinnsvurdering != null) oppgaveMediator.lagreTotrinnsvurdering(totrinnsvurdering)
    }

    internal fun oppdater(oppgaveMediator: OppgaveMediator) {
        val oppgave = oppgaveForLagring
        oppgaveMediator.oppdater(
            oppgaveId = oppgave.id,
            status = oppgave.status,
            ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
            ferdigstiltAvOid = oppgave.ferdigstiltAvOid,
            egenskaper = oppgave.egenskaper,
        )
        if (oppgave.tildelt != null) tildelingDao.tildel(oppgave.id, oppgave.tildelt.oid, oppgave.påVent)
        else tildelingDao.avmeld(oppgave.id)

        val totrinnsvurdering = totrinnsvurderingForLagring
        if (totrinnsvurdering != null) oppgaveMediator.lagreTotrinnsvurdering(totrinnsvurdering)
    }

    override fun visitOppgave(
        id: Long,
        egenskap: Egenskap,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Egenskap>,
        tildelt: Saksbehandler?,
        påVent: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {
        oppgaveForLagring = OppgaveFraDatabase(
            id = id,
            egenskap = egenskap(egenskap),
            egenskaper = egenskaper.map { egenskap(it) },
            status = status(tilstand),
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            hendelseId = hendelseId,
            ferdigstiltAvIdent = ferdigstiltAvIdent,
            ferdigstiltAvOid = ferdigstiltAvOid,
            tildelt = tildelt?.let { Saksbehandlerhenter(it).hent() },
            påVent = påVent
        )
    }

    override fun visitTotrinnsvurdering(
        vedtaksperiodeId: UUID,
        erRetur: Boolean,
        saksbehandler: Saksbehandler?,
        beslutter: Saksbehandler?,
        utbetalingId: UUID?,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime?
    ) {
        totrinnsvurderingForLagring = TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = erRetur,
            saksbehandler = saksbehandler?.oid(),
            beslutter = beslutter?.oid(),
            utbetalingId = utbetalingId,
            opprettet = opprettet,
            oppdatert = oppdatert
        )
    }

    private fun status(tilstand: Oppgave.Tilstand): String {
        return when (tilstand) {
            Oppgave.AvventerSaksbehandler -> "AvventerSaksbehandler"
            Oppgave.AvventerSystem -> "AvventerSystem"
            Oppgave.Ferdigstilt -> "Ferdigstilt"
            Oppgave.Invalidert -> "Invalidert"
        }
    }

    private fun egenskap(egenskap: Egenskap): String {
        return when (egenskap) {
            DELVIS_REFUSJON -> "DELVIS_REFUSJON"
            INGEN_UTBETALING -> "INGEN_UTBETALING"
            REVURDERING -> "REVURDERING"
            STIKKPRØVE -> "STIKKPRØVE"
            SØKNAD -> "SØKNAD"
            EGEN_ANSATT -> "EGEN_ANSATT"
            FORTROLIG_ADRESSE -> "FORTROLIG_ADRESSE"
            RISK_QA -> "RISK_QA"
            UTBETALING_TIL_ARBEIDSGIVER -> "UTBETALING_TIL_ARBEIDSGIVER"
            UTBETALING_TIL_SYKMELDT -> "UTBETALING_TIL_SYKMELDT"
            HASTER -> "HASTER"
            BESLUTTER -> "BESLUTTER"
            RETUR -> "RETUR"
            FULLMAKT -> "FULLMAKT"
            VERGEMÅL -> "VERGEMÅL"
            SPESIALSAK -> "SPESIALSAK"
        }
    }

    private class Saksbehandlerhenter(saksbehandler: Saksbehandler): SaksbehandlerVisitor {
        private lateinit var saksbehandlerFraDatabase: SaksbehandlerFraDatabase
        init {
            saksbehandler.accept(this)
        }

        override fun visitSaksbehandler(epostadresse: String, oid: UUID, navn: String, ident: String) {
            saksbehandlerFraDatabase = SaksbehandlerFraDatabase(epostadresse, oid, navn, ident)
        }

        fun hent() = saksbehandlerFraDatabase
    }
}