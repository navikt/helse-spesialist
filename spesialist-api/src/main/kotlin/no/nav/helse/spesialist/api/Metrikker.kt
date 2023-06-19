package no.nav.helse.spesialist.api

import io.prometheus.client.Counter

private val overstyringsteller: Counter = Counter.build("overstyringer", "Teller antall overstyringer")
    .labelNames("opplysningstype", "type")
    .register()

private val annulleringsteller = Counter.build("annulleringer", "Teller antall annulleringer")
    .register()

internal fun tellAnnullering() = annulleringsteller.inc()
internal fun tellOverstyrTidslinje() = overstyringsteller.labels("opplysningstype", "tidslinje").inc()
internal fun tellOverstyrArbeidsforhold() = overstyringsteller.labels("opplysningstype", "arbeidsforhold").inc()
internal fun tellOverstyrInntektOgRefusjon() = overstyringsteller.labels("opplysningstype", "inntektogrefusjon").inc()
internal fun tellSkjønnsmessigFastsettingInntekt() = overstyringsteller.labels("opplysningstype", "skjønnsmessigfastsettinginntekt").inc()