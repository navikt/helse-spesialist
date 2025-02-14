package no.nav.helse.mediator.overstyring

import no.nav.helse.MeldingPubliserer
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.melding.LagtPåVentEvent
import no.nav.helse.modell.melding.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.melding.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.melding.OverstyrtTidslinjeEvent
import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver

class Saksbehandlingsmelder(
    private val meldingPubliserer: MeldingPubliserer,
) : SaksbehandlerObserver {
    override fun tidslinjeOverstyrt(
        fødselsnummer: String,
        event: OverstyrtTidslinjeEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "overstyring av tidslinje")
    }

    override fun arbeidsforholdOverstyrt(
        fødselsnummer: String,
        event: OverstyrtArbeidsforholdEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "overstyring av arbeidsforhold")
    }

    override fun inntektOgRefusjonOverstyrt(
        fødselsnummer: String,
        event: OverstyrtInntektOgRefusjonEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "overstyring av inntekt og refusjon")
    }

    override fun sykepengegrunnlagSkjønnsfastsatt(
        fødselsnummer: String,
        event: SkjønnsfastsattSykepengegrunnlagEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "fastsettelse av sykepengegrunnlag")
    }

    override fun minimumSykdomsgradVurdert(
        fødselsnummer: String,
        event: MinimumSykdomsgradVurdertEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "vurdering av minimum sykdomsgrad")
    }

    override fun utbetalingAnnullert(
        fødselsnummer: String,
        event: AnnullertUtbetalingEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "annullering av utbetaling")
    }

    override fun lagtPåVent(
        fødselsnummer: String,
        event: LagtPåVentEvent,
    ) {
        meldingPubliserer.publiser(fødselsnummer, event, "legging på vent")
    }
}
