package no.nav.helse.spesialist.api

import io.prometheus.client.Counter
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SaksbehandlerHandling

private val overstyringsteller: Counter = Counter.build("overstyringer", "Teller antall overstyringer")
    .labelNames("opplysningstype", "type")
    .register()

private val annulleringsteller = Counter.build("annulleringer", "Teller antall annulleringer")
    .register()

internal fun tellAnnullering() = annulleringsteller.inc()
internal fun tellOverstyrTidslinje() = overstyringsteller.labels("opplysningstype", "tidslinje").inc()
internal fun tellOverstyrArbeidsforhold() = overstyringsteller.labels("opplysningstype", "arbeidsforhold").inc()
internal fun tellOverstyrInntektOgRefusjon() = overstyringsteller.labels("opplysningstype", "inntektogrefusjon").inc()
internal fun tellSkjønnsfastsettingSykepengegrunnlag() = overstyringsteller.labels("opplysningstype", "skjønnsfastsettingsykepengegrunnlag").inc()

internal fun tell(handling: SaksbehandlerHandling) = when (handling) {
    is OverstyrTidslinjeHandling -> tellOverstyrTidslinje()
    is OverstyrInntektOgRefusjonHandling -> tellOverstyrInntektOgRefusjon()
    is OverstyrArbeidsforholdHandling -> tellOverstyrArbeidsforhold()
}