package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.delvisRefusjon
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.slf4j.LoggerFactory

internal class OpprettSaksbehandleroppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveMediator: OppgaveMediator,
    private val automatisering: Automatisering,
    private val hendelseId: UUID,
    private val personDao: PersonDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val utbetalingId: UUID,
    private val utbetalingtype: Utbetalingtype,
    private val snapshotMediator: SnapshotMediator,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgaveCommand::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val egenskaper = mutableListOf<Oppgavetype>()
        if (harFortroligAdressebeskyttelse) egenskaper.add(Oppgavetype.FORTROLIG_ADRESSE)
        if (utbetalingtype == Utbetalingtype.REVURDERING) egenskaper.add(Oppgavetype.REVURDERING)
        if (automatisering.erStikkprøve(vedtaksperiodeId, hendelseId)) egenskaper.add(Oppgavetype.STIKKPRØVE)
        if (risikovurderingDao.kreverSupersaksbehandler(vedtaksperiodeId)) egenskaper.add(Oppgavetype.RISK_QA)

        if (vedtaksperiodensUtbetaling.delvisRefusjon()) egenskaper.add(Oppgavetype.DELVIS_REFUSJON)
        else if (vedtaksperiodensUtbetaling.utbetalingTilSykmeldt()) egenskaper.add(Oppgavetype.UTBETALING_TIL_SYKMELDT)

        // Kommentert ut fordi disse typene finnes ikke i Speil enda
//        else if (vedtaksperiodensUtbetaling.utbetalingTilArbeidsgiver()) egenskaper.add(Oppgavetype.UTBETALING_TIL_ARBEIDSGIVER)
//        else egenskaper.add(Oppgavetype.INGEN_UTBETALING)

        val oppgave = Oppgave.oppgaveMedEgenskaper(vedtaksperiodeId, utbetalingId, egenskaper)

        logg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
        sikkerLogg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
        oppgaveMediator.opprett(oppgave)
        return true
    }

    private val harFortroligAdressebeskyttelse
        get() =
            personDao.findAdressebeskyttelse(fødselsnummer) == Adressebeskyttelse.Fortrolig

    private val vedtaksperiodensUtbetaling by lazy { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) }
}
