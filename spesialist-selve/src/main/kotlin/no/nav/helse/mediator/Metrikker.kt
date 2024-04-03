package no.nav.helse.mediator

import io.prometheus.client.Counter
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag

private val overstyringsteller: Counter =
    Counter.build("overstyringer", "Teller antall overstyringer")
        .labelNames("opplysningstype", "type")
        .register()

private val annulleringsteller =
    Counter.build("annulleringer", "Teller antall annulleringer")
        .register()

internal fun tellAnnullering() = annulleringsteller.inc()

internal fun tellOverstyrTidslinje() = overstyringsteller.labels("opplysningstype", "tidslinje").inc()

internal fun tellOverstyrArbeidsforhold() = overstyringsteller.labels("opplysningstype", "arbeidsforhold").inc()

internal fun tellOverstyrInntektOgRefusjon() = overstyringsteller.labels("opplysningstype", "inntektogrefusjon").inc()

internal fun tellSkjønnsfastsettingSykepengegrunnlag() =
    overstyringsteller.labels(
        "opplysningstype",
        "skjønnsfastsettingsykepengegrunnlag",
    ).inc()

fun tell(handling: Handling) =
    when (handling) {
        is OverstyrtTidslinje -> tellOverstyrTidslinje()
        is OverstyrtInntektOgRefusjon -> tellOverstyrInntektOgRefusjon()
        is OverstyrtArbeidsforhold -> tellOverstyrArbeidsforhold()
        is SkjønnsfastsattSykepengegrunnlag -> tellSkjønnsfastsettingSykepengegrunnlag()
        is Annullering -> tellAnnullering()
        else -> {}
    }
