package no.nav.helse.modell.saksbehandler

import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.melding.LagtPåVentEvent
import no.nav.helse.modell.melding.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.melding.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.melding.OverstyrtTidslinjeEvent
import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.melding.SubsumsjonEvent

interface SaksbehandlerObserver {
    fun tidslinjeOverstyrt(
        fødselsnummer: String,
        event: OverstyrtTidslinjeEvent,
    ) {}

    fun inntektOgRefusjonOverstyrt(
        fødselsnummer: String,
        event: OverstyrtInntektOgRefusjonEvent,
    ) {}

    fun arbeidsforholdOverstyrt(
        fødselsnummer: String,
        event: OverstyrtArbeidsforholdEvent,
    ) {}

    fun sykepengegrunnlagSkjønnsfastsatt(
        fødselsnummer: String,
        event: SkjønnsfastsattSykepengegrunnlagEvent,
    ) {}

    fun minimumSykdomsgradVurdert(
        fødselsnummer: String,
        event: MinimumSykdomsgradVurdertEvent,
    ) {}

    fun utbetalingAnnullert(
        fødselsnummer: String,
        event: AnnullertUtbetalingEvent,
    ) {}

    fun lagtPåVent(
        fødselsnummer: String,
        event: LagtPåVentEvent,
    ) {}

    fun nySubsumsjon(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
    ) {}
}
