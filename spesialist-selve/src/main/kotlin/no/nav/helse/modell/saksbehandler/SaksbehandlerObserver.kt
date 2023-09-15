package no.nav.helse.modell.saksbehandler

import no.nav.helse.modell.saksbehandler.handlinger.SubsumsjonEvent
import no.nav.helse.spesialist.api.modell.AnnullertUtbetalingEvent
import no.nav.helse.spesialist.api.modell.OverstyrtArbeidsforholdEvent
import no.nav.helse.spesialist.api.modell.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.spesialist.api.modell.OverstyrtTidslinjeEvent
import no.nav.helse.spesialist.api.modell.SkjønnsfastsattSykepengegrunnlagEvent

interface SaksbehandlerObserver {
    fun tidslinjeOverstyrt(fødselsnummer: String, event: OverstyrtTidslinjeEvent) {}
    fun inntektOgRefusjonOverstyrt(fødselsnummer: String, event: OverstyrtInntektOgRefusjonEvent) {}
    fun arbeidsforholdOverstyrt(fødselsnummer: String, event: OverstyrtArbeidsforholdEvent) {}
    fun sykepengegrunnlagSkjønnsfastsatt(fødselsnummer: String, event: SkjønnsfastsattSykepengegrunnlagEvent) {}
    fun utbetalingAnnullert(fødselsnummer: String, event: AnnullertUtbetalingEvent) {}
    fun nySubsumsjon(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent) {}
}