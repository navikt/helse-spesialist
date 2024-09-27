package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.GOSYS
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.MEDLEMSKAP
import no.nav.helse.modell.oppgave.Egenskap.OVERGANG_FRA_IT
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SKJØNNSFASTSETTELSE
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.TILBAKEDATERT
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.oppgave.Egenskap.VERGEMÅL
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.slf4j.LoggerFactory

internal class OpprettSaksbehandleroppgave(
    private val behovData: GodkjenningsbehovData,
    private val oppgaveService: OppgaveService,
    private val automatisering: Automatisering,
    private val personRepository: PersonRepository,
    private val risikovurderingDao: RisikovurderingDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val utbetalingtype: Utbetalingtype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val utbetaling: Utbetaling,
    private val vergemålDao: VergemålDao,
    private val vedtakDao: VedtakDao,
    private val påVentDao: PåVentDao,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgave::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val egenskaper = mutableListOf<Egenskap>()
        val fødselsnummer = behovData.fødselsnummer
        val vedtaksperiodeId = behovData.vedtaksperiodeId
        val hendelseId = behovData.id
        val utbetalingId = utbetaling.utbetalingId
        val periodetype = behovData.periodetype
        val inntektskilde = behovData.inntektskilde
        val kanAvvises = behovData.kanAvvises

        if (egenAnsattDao.erEgenAnsatt(fødselsnummer) == true) egenskaper.add(EGEN_ANSATT)

        val adressebeskyttelse = personRepository.finnAdressebeskyttelse(fødselsnummer)
        when (adressebeskyttelse) {
            Adressebeskyttelse.StrengtFortrolig,
            Adressebeskyttelse.StrengtFortroligUtland,
            -> egenskaper.add(STRENGT_FORTROLIG_ADRESSE)
            Adressebeskyttelse.Fortrolig -> egenskaper.add(FORTROLIG_ADRESSE)
            else -> {}
        }

        if (utbetalingtype == Utbetalingtype.REVURDERING) {
            egenskaper.add(REVURDERING)
        } else {
            egenskaper.add(SØKNAD)
        }

        if (automatisering.erStikkprøve(vedtaksperiodeId, hendelseId)) egenskaper.add(STIKKPRØVE)
        if (!egenskaper.contains(REVURDERING) && risikovurderingDao.kreverSupersaksbehandler(vedtaksperiodeId)) egenskaper.add(RISK_QA)
        if (vergemålDao.harVergemål(fødselsnummer) == true) egenskaper.add(VERGEMÅL)
        if (HentEnhetløsning.erEnhetUtland(personRepository.finnEnhetId(fødselsnummer))) egenskaper.add(UTLAND)

        when {
            utbetaling.delvisRefusjon() -> egenskaper.add(DELVIS_REFUSJON)
            utbetaling.kunUtbetalingTilSykmeldt() -> egenskaper.add(UTBETALING_TIL_SYKMELDT)
            utbetaling.kunUtbetalingTilArbeidsgiver() -> egenskaper.add(UTBETALING_TIL_ARBEIDSGIVER)
            utbetaling.ingenUtbetaling() -> egenskaper.add(INGEN_UTBETALING)
        }

        when (inntektskilde) {
            Inntektskilde.EN_ARBEIDSGIVER -> egenskaper.add(EN_ARBEIDSGIVER)
            Inntektskilde.FLERE_ARBEIDSGIVERE -> egenskaper.add(FLERE_ARBEIDSGIVERE)
        }

        when (periodetype) {
            Periodetype.FØRSTEGANGSBEHANDLING -> egenskaper.add(FORSTEGANGSBEHANDLING)
            Periodetype.FORLENGELSE -> egenskaper.add(FORLENGELSE)
            Periodetype.INFOTRYGDFORLENGELSE -> egenskaper.add(INFOTRYGDFORLENGELSE)
            Periodetype.OVERGANG_FRA_IT -> egenskaper.add(OVERGANG_FRA_IT)
        }

        if (vedtakDao.erSpesialsak(vedtaksperiodeId)) {
            egenskaper.add(SPESIALSAK)
        }

        if (påVentDao.erPåVent(vedtaksperiodeId)) {
            egenskaper.add(PÅ_VENT)
        }

        if (sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId)) egenskaper.add(SKJØNNSFASTSETTELSE)

        if (sykefraværstilfelle.erTilbakedatert(vedtaksperiodeId)) egenskaper.add(TILBAKEDATERT)

        if (sykefraværstilfelle.harKunÅpenGosysOppgave(vedtaksperiodeId)) egenskaper.add(GOSYS)

        if (sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId)) egenskaper.add(MEDLEMSKAP)

        if (sykefraværstilfelle.haster(vedtaksperiodeId) && utbetaling.harEndringIUtbetalingTilSykmeldt()) egenskaper.add(HASTER)

        oppgaveService.nyOppgave(fødselsnummer, context.id()) { reservertId ->
            val oppgave = Oppgave.nyOppgave(reservertId, vedtaksperiodeId, utbetalingId, hendelseId, kanAvvises, egenskaper)

            logg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
            sikkerLogg.info("Saksbehandleroppgave opprettet, avventer lagring: $oppgave")
            oppgave
        }

        return true
    }
}
