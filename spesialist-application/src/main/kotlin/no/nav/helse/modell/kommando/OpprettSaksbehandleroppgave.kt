package no.nav.helse.modell.kommando

import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.GOSYS
import no.nav.helse.modell.oppgave.Egenskap.GRUNNBELØPSREGULERING
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.MANGLER_IM
import no.nav.helse.modell.oppgave.Egenskap.MEDLEMSKAP
import no.nav.helse.modell.oppgave.Egenskap.OVERGANG_FRA_IT
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SKJØNNSFASTSETTELSE
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.TILBAKEDATERT
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.oppgave.Egenskap.VERGEMÅL
import no.nav.helse.modell.person.Adressebeskyttelse.Fortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortroligUtland
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import java.util.UUID

internal class OpprettSaksbehandleroppgave(
    private val behovData: GodkjenningsbehovData,
    private val oppgaveService: OppgaveService,
    private val automatisering: Automatisering,
    private val personDao: PersonDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val utbetalingtype: Utbetalingtype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val utbetaling: Utbetaling,
    private val vergemålDao: VergemålDao,
    private val påVentDao: PåVentDao,
    private val opptegnelseDao: OpptegnelseDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val fødselsnummer = behovData.fødselsnummer
        val vedtaksperiodeId = behovData.vedtaksperiodeId
        val hendelseId = behovData.id
        val utbetalingId = behovData.utbetalingId
        val periodetype = behovData.periodetype
        val inntektskilde = behovData.inntektskilde
        val kanAvvises = behovData.kanAvvises

        val egenskaper =
            buildSet {
                egenAnsatt(fødselsnummer)
                adressebeskyttelse(fødselsnummer)
                oppgavetype(utbetalingtype)
                stikkprøve(vedtaksperiodeId, hendelseId)
                vurderingsmomenter(vedtaksperiodeId, utbetalingtype)
                vergemål(fødselsnummer)
                enhetUtland(fødselsnummer)
                mottaker()
                inntektskilde(inntektskilde)
                periodetype(periodetype)
                påVent(vedtaksperiodeId)
                skjønnsfastsettelse(vedtaksperiodeId)
                tilbakedatert(vedtaksperiodeId)
                kunÅpenGosysOppgave(vedtaksperiodeId)
                manglerIM(vedtaksperiodeId)
                medlemskap(vedtaksperiodeId)
                haster(vedtaksperiodeId)
                grunnbeløpsregulering(behovData.tags, utbetalingtype)
            }

        val behandlingId = behovData.spleisBehandlingId
        oppgaveService.nyOppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = hendelseId,
            kanAvvises = kanAvvises,
            egenskaper = egenskaper,
        )
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer,
            GodkjenningsbehovPayload(behovData.id).toJson(),
            OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
        )
        return true
    }

    private fun MutableSet<Egenskap>.egenAnsatt(fødselsnummer: String) {
        if (egenAnsattDao.erEgenAnsatt(fødselsnummer) == true) add(EGEN_ANSATT)
    }

    private fun MutableSet<Egenskap>.adressebeskyttelse(fødselsnummer: String) {
        when (personDao.finnAdressebeskyttelse(fødselsnummer)) {
            StrengtFortrolig,
            StrengtFortroligUtland,
            -> add(STRENGT_FORTROLIG_ADRESSE)
            Fortrolig -> add(FORTROLIG_ADRESSE)
            else -> Unit
        }
    }

    private fun MutableSet<Egenskap>.oppgavetype(utbetalingtype: Utbetalingtype) {
        add(if (utbetalingtype == Utbetalingtype.REVURDERING) REVURDERING else SØKNAD)
    }

    private fun MutableSet<Egenskap>.stikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        if (automatisering.erStikkprøve(vedtaksperiodeId, hendelseId)) add(STIKKPRØVE)
    }

    private fun MutableSet<Egenskap>.vurderingsmomenter(
        vedtaksperiodeId: UUID,
        utbetalingtype: Utbetalingtype,
    ) {
        if (utbetalingtype != Utbetalingtype.REVURDERING && risikovurderingDao.måTilManuell(vedtaksperiodeId)) {
            add(RISK_QA)
        }
    }

    private fun MutableSet<Egenskap>.vergemål(fødselsnummer: String) {
        if (vergemålDao.harVergemål(fødselsnummer) == true) add(VERGEMÅL)
    }

    private fun MutableSet<Egenskap>.enhetUtland(fødselsnummer: String) {
        if (HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))) add(UTLAND)
    }

    private fun MutableSet<Egenskap>.mottaker() {
        when {
            utbetaling.delvisRefusjon() -> add(DELVIS_REFUSJON)
            utbetaling.kunUtbetalingTilSykmeldt() -> add(UTBETALING_TIL_SYKMELDT)
            utbetaling.kunUtbetalingTilArbeidsgiver() -> add(UTBETALING_TIL_ARBEIDSGIVER)
            utbetaling.ingenUtbetaling() -> add(INGEN_UTBETALING)
            else -> Unit
        }
    }

    private fun MutableSet<Egenskap>.inntektskilde(inntektskilde: Inntektskilde) {
        when (inntektskilde) {
            Inntektskilde.EN_ARBEIDSGIVER -> add(EN_ARBEIDSGIVER)
            Inntektskilde.FLERE_ARBEIDSGIVERE -> add(FLERE_ARBEIDSGIVERE)
        }
    }

    private fun MutableSet<Egenskap>.periodetype(periodetype: Periodetype) {
        when (periodetype) {
            Periodetype.FØRSTEGANGSBEHANDLING -> add(FORSTEGANGSBEHANDLING)
            Periodetype.FORLENGELSE -> add(FORLENGELSE)
            Periodetype.INFOTRYGDFORLENGELSE -> add(INFOTRYGDFORLENGELSE)
            Periodetype.OVERGANG_FRA_IT -> add(OVERGANG_FRA_IT)
        }
    }

    private fun MutableSet<Egenskap>.påVent(vedtaksperiodeId: UUID) {
        if (påVentDao.erPåVent(vedtaksperiodeId)) add(PÅ_VENT)
    }

    private fun MutableSet<Egenskap>.skjønnsfastsettelse(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.kreverSkjønnsfastsettelse(vedtaksperiodeId)) add(SKJØNNSFASTSETTELSE)
    }

    private fun MutableSet<Egenskap>.tilbakedatert(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.erTilbakedatert(vedtaksperiodeId)) add(TILBAKEDATERT)
    }

    private fun MutableSet<Egenskap>.kunÅpenGosysOppgave(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.harKunÅpenGosysOppgave(vedtaksperiodeId)) add(GOSYS)
    }

    private fun MutableSet<Egenskap>.medlemskap(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId)) add(MEDLEMSKAP)
    }

    private fun MutableSet<Egenskap>.manglerIM(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.harVarselOmManglendeInntektsmelding(vedtaksperiodeId)) add(MANGLER_IM)
    }

    private fun MutableSet<Egenskap>.grunnbeløpsregulering(
        tags: List<String>,
        utbetalingtype: Utbetalingtype,
    ) {
        if (tags.contains("Grunnbeløpsregulering") and (utbetalingtype == Utbetalingtype.REVURDERING)) add(GRUNNBELØPSREGULERING)
    }

    private fun MutableSet<Egenskap>.haster(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.haster(vedtaksperiodeId) && utbetaling.harEndringIUtbetalingTilSykmeldt()) add(HASTER)
    }
}
