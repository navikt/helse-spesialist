package no.nav.helse.modell.kommando

import io.opentelemetry.api.trace.Span
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.person.Adressebeskyttelse.*
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.*
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.*
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Egenskap.*
import no.nav.helse.spesialist.domain.oppgave.Inntektsforhold
import no.nav.helse.spesialist.domain.oppgave.Mottaker
import no.nav.helse.spesialist.domain.oppgave.Oppgavetype
import java.util.*

internal class OpprettSaksbehandleroppgave(
    private val behovData: GodkjenningsbehovData,
    private val oppgaveService: OppgaveService,
    private val automatisering: Automatisering,
    private val utbetalingtype: Utbetalingtype,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val utbetaling: Utbetaling,
    private val forsikringsvurderingHenter: ForsikringsvurderingHenter,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val fødselsnummer = behovData.fødselsnummer
        val vedtaksperiodeId = behovData.vedtaksperiodeId
        val hendelseId = behovData.id
        val utbetalingId = behovData.utbetalingId
        val periodetype = behovData.periodetype
        val inntektskilde = behovData.inntektskilde
        val kanAvvises = behovData.kanAvvises
        val oppgavetype = if (utbetalingtype == Utbetalingtype.REVURDERING) Oppgavetype.Revurdering else Oppgavetype.Søknad
        val mottaker =
            when {
                utbetaling.delvisRefusjon() -> Mottaker.DelvisRefusjon
                utbetaling.kunUtbetalingTilSykmeldt() -> Mottaker.UtbetalingTilSykmeldt
                utbetaling.kunUtbetalingTilArbeidsgiver() -> Mottaker.UtbetalingTilArbeidsgiver
                else -> Mottaker.IngenUtbetaling
            }
        val inntektsforhold =
            when (behovData.yrkesaktivitetstype) {
                Yrkesaktivitetstype.SELVSTENDIG -> Inntektsforhold.SelvstendigNæringsdrivende

                Yrkesaktivitetstype.ARBEIDSTAKER -> Inntektsforhold.Arbeidstaker

                Yrkesaktivitetstype.FRILANS,
                Yrkesaktivitetstype.ARBEIDSLEDIG,
                -> error("Støtter ikke yrkesaktivitetstype ${behovData.yrkesaktivitetstype}")
            }

        val egenskaper =
            buildSet {
                egenAnsatt(fødselsnummer, sessionContext)
                adressebeskyttelse(fødselsnummer, sessionContext)
                oppgavetype(oppgavetype)
                stikkprøve(vedtaksperiodeId, hendelseId)
                vurderingsmomenter(vedtaksperiodeId, utbetalingtype, sessionContext)
                vergemål(fødselsnummer, sessionContext)
                enhetUtland(fødselsnummer, sessionContext)
                mottaker(mottaker)
                inntektskilde(inntektskilde)
                inntektsforhold(inntektsforhold)
                arbeidssituasjon(behovData.arbeidssituasjon)
                periodetype(periodetype)
                påVent(vedtaksperiodeId, sessionContext)
                skjønnsfastsettelse(vedtaksperiodeId)
                tilbakedatert(vedtaksperiodeId)
                kunÅpenGosysOppgave(vedtaksperiodeId)
                manglerIM(vedtaksperiodeId)
                medlemskap(vedtaksperiodeId)
                haster(vedtaksperiodeId)
                grunnbeløpsregulering(behovData.tags, utbetalingtype)
                forsikring(behovData.forsikringsvurderingId)
            }

        val behandlingId = behovData.spleisBehandlingId
        oppgaveService.nyOppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
            behandlingId = SpleisBehandlingId(behandlingId),
            utbetalingId = utbetalingId,
            hendelseId = hendelseId,
            kanAvvises = kanAvvises,
            egenskaper = egenskaper,
            mottaker = mottaker,
            type = oppgavetype,
            inntektskilde = inntektskilde,
            inntektsforhold = inntektsforhold,
            periodetype = periodetype,
        )
        val opptegnelse =
            Opptegnelse.ny(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
            )
        Span.current().setAttribute("speil.saksbehandling.spesialist", "oppgave_opprettet")
        sessionContext.opptegnelseRepository.lagre(opptegnelse)
        return true
    }

    private fun MutableSet<Egenskap>.egenAnsatt(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ) {
        if (sessionContext.personRepository
                .finn(Identitetsnummer.fraString(fødselsnummer))
                ?.egenAnsattStatus
                ?.erEgenAnsatt == true
        ) {
            add(EGEN_ANSATT)
        }
    }

    private fun MutableSet<Egenskap>.adressebeskyttelse(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ) {
        when (sessionContext.personDao.finnAdressebeskyttelse(fødselsnummer)) {
            StrengtFortrolig,
            StrengtFortroligUtland,
            -> add(STRENGT_FORTROLIG_ADRESSE)

            Fortrolig -> add(FORTROLIG_ADRESSE)

            else -> Unit
        }
    }

    private fun MutableSet<Egenskap>.oppgavetype(oppgavetype: Oppgavetype) {
        add(if (oppgavetype == Oppgavetype.Revurdering) REVURDERING else SØKNAD)
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
        sessionContext: SessionContext,
    ) {
        if (utbetalingtype != Utbetalingtype.REVURDERING && sessionContext.risikovurderingDao.måTilManuell(vedtaksperiodeId)) {
            add(RISK_QA)
        }
    }

    private fun MutableSet<Egenskap>.vergemål(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ) {
        if (sessionContext.vergemålDao.harVergemål(fødselsnummer) == true) add(VERGEMÅL)
    }

    private fun MutableSet<Egenskap>.enhetUtland(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ) {
        if (HentEnhetløsning.erEnhetUtland(sessionContext.personDao.finnEnhetId(fødselsnummer))) add(UTLAND)
    }

    private fun MutableSet<Egenskap>.arbeidssituasjon(arbeidssituasjon: Arbeidssituasjon?) {
        if (arbeidssituasjon == Arbeidssituasjon.JORDBRUKER) add(Egenskap.JORDBRUKER_REINDRIFT)
    }

    private fun MutableSet<Egenskap>.mottaker(mottaker: Mottaker) {
        when (mottaker) {
            Mottaker.UtbetalingTilSykmeldt -> add(UTBETALING_TIL_SYKMELDT)
            Mottaker.DelvisRefusjon -> add(DELVIS_REFUSJON)
            Mottaker.UtbetalingTilArbeidsgiver -> add(UTBETALING_TIL_ARBEIDSGIVER)
            Mottaker.IngenUtbetaling -> add(INGEN_UTBETALING)
        }
    }

    private fun MutableSet<Egenskap>.inntektskilde(inntektskilde: Inntektskilde) {
        when (inntektskilde) {
            Inntektskilde.EN_ARBEIDSGIVER -> add(EN_ARBEIDSGIVER)
            Inntektskilde.FLERE_ARBEIDSGIVERE -> add(FLERE_ARBEIDSGIVERE)
        }
    }

    private fun MutableSet<Egenskap>.inntektsforhold(inntektsforhold: Inntektsforhold) {
        when (inntektsforhold) {
            Inntektsforhold.SelvstendigNæringsdrivende -> add(SELVSTENDIG_NÆRINGSDRIVENDE)
            Inntektsforhold.Arbeidstaker -> add(ARBEIDSTAKER)
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

    private fun MutableSet<Egenskap>.påVent(
        vedtaksperiodeId: UUID,
        sessionContext: SessionContext,
    ) {
        if (sessionContext.påVentDao.erPåVent(vedtaksperiodeId)) add(PÅ_VENT)
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
        if (tags.contains("Grunnbeløpsregulering") and (utbetalingtype == Utbetalingtype.REVURDERING)) {
            add(
                GRUNNBELØPSREGULERING,
            )
        }
    }

    private fun MutableSet<Egenskap>.haster(vedtaksperiodeId: UUID) {
        if (sykefraværstilfelle.haster(vedtaksperiodeId) && utbetaling.harEndringIUtbetalingTilSykmeldt()) add(HASTER)
    }

    private fun MutableSet<Egenskap>.forsikring(forsikringsvurderingId: UUID?) {
        if (forsikringsvurderingId == null) return
        val forsikringsvurdering = forsikringsvurderingHenter.hent(ForsikringsvurderingId(forsikringsvurderingId))
        if (forsikringsvurdering?.harForsikring() == true) add(FORSIKRING)
    }
}
