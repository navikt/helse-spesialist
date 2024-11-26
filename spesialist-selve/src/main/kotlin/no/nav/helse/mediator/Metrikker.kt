package no.nav.helse.mediator

// import io.micrometer.core.instrument.Counter
import no.nav.helse.modell.saksbehandler.handlinger.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.Handling
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag

// private val overstyringsteller: Counter =
//    Counter.build("overstyringer", "Teller antall overstyringer")
//        .labelNames("opplysningstype", "type")
//        .register()
//
// private val annulleringsteller =
//    Counter.build("annulleringer", "Teller antall annulleringer")
//        .register()

internal fun tellAnnullering() = Unit

internal fun tellOverstyrTidslinje() = Unit

internal fun tellOverstyrArbeidsforhold() = Unit

internal fun tellOverstyrInntektOgRefusjon() = Unit

internal fun tellSkjønnsfastsettingSykepengegrunnlag() = Unit

fun tell(handling: Handling) =
    when (handling) {
        is OverstyrtTidslinje -> tellOverstyrTidslinje()
        is OverstyrtInntektOgRefusjon -> tellOverstyrInntektOgRefusjon()
        is OverstyrtArbeidsforhold -> tellOverstyrArbeidsforhold()
        is SkjønnsfastsattSykepengegrunnlag -> tellSkjønnsfastsettingSykepengegrunnlag()
        is Annullering -> tellAnnullering()
        else -> {}
    }
