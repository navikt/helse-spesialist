package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.delvisRefusjon
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.oppgave.Egenskap.VERGEMÅL
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetalingTilArbeidsgiver
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vergemal.VergemålDao
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
    private val egenAnsattDao: EgenAnsattDao,
    private val utbetalingId: UUID,
    private val utbetalingtype: Utbetalingtype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val snapshotMediator: SnapshotMediator,
    private val vergemålDao: VergemålDao,
    private val inntektskilde: Inntektskilde,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgaveCommand::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val egenskaper = mutableListOf<Egenskap>()
        if (Toggle.EgenAnsatt.enabled && egenAnsattDao.erEgenAnsatt(fødselsnummer) == true) egenskaper.add(EGEN_ANSATT)
        if (harFortroligAdressebeskyttelse) egenskaper.add(FORTROLIG_ADRESSE)

        if (utbetalingtype == Utbetalingtype.REVURDERING) egenskaper.add(REVURDERING)
        else egenskaper.add(SØKNAD)

        if (automatisering.erStikkprøve(vedtaksperiodeId, hendelseId)) egenskaper.add(STIKKPRØVE)
        if (!egenskaper.contains(REVURDERING) && risikovurderingDao.kreverSupersaksbehandler(vedtaksperiodeId)) egenskaper.add(RISK_QA)
        if (vergemålDao.harVergemål(fødselsnummer) == true) egenskaper.add(VERGEMÅL)
        if (HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))) egenskaper.add(UTLAND)

        when {
            vedtaksperiodensUtbetaling.delvisRefusjon() -> egenskaper.add(DELVIS_REFUSJON)
            vedtaksperiodensUtbetaling.utbetalingTilSykmeldt() -> egenskaper.add(UTBETALING_TIL_SYKMELDT)
            vedtaksperiodensUtbetaling.utbetalingTilArbeidsgiver() -> egenskaper.add(UTBETALING_TIL_ARBEIDSGIVER)
            else -> egenskaper.add(INGEN_UTBETALING)
        }

        when (inntektskilde) {
            Inntektskilde.EN_ARBEIDSGIVER -> egenskaper.add(EN_ARBEIDSGIVER)
            Inntektskilde.FLERE_ARBEIDSGIVERE -> egenskaper.add(FLERE_ARBEIDSGIVERE)
        }

        if (sykefraværstilfelle.haster(vedtaksperiodeId)) egenskaper.add(HASTER)

        oppgaveMediator.nyOppgave(fødselsnummer, context.id()) { reservertId ->
            val oppgave = Oppgave.nyOppgave(reservertId, vedtaksperiodeId, utbetalingId, hendelseId, egenskaper)

            logg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
            sikkerLogg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
            oppgave
        }

        return true
    }

    private val harFortroligAdressebeskyttelse
        get() =
            personDao.findAdressebeskyttelse(fødselsnummer) == Adressebeskyttelse.Fortrolig

    private val vedtaksperiodensUtbetaling by lazy { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) }
}
