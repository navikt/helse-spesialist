package no.nav.helse.spesialist.api

import io.prometheus.client.Counter
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi

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

fun tell(handling: HandlingFraApi) = when (handling) {
    is OverstyrTidslinjeHandlingFraApi -> tellOverstyrTidslinje()
    is OverstyrInntektOgRefusjonHandlingFraApi -> tellOverstyrInntektOgRefusjon()
    is OverstyrArbeidsforholdHandlingFraApi -> tellOverstyrArbeidsforhold()
    is SkjønnsfastsettSykepengegrunnlagHandlingFraApi -> tellSkjønnsfastsettingSykepengegrunnlag()
    is AnnulleringHandlingFraApi -> tellAnnullering()
}