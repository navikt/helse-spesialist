package no.nav.helse.spesialist.api

import io.prometheus.client.Counter

//private val overstyringsteller = Counter.build("overstyringer", "Teller antall overstyringer")
//    .labelNames("opplysningstype", "type")
//    .register()

private val annulleringsteller = Counter.build("annulleringer", "Teller antall annulleringer")
    .register()

internal fun tellAnnullering() = annulleringsteller.inc()