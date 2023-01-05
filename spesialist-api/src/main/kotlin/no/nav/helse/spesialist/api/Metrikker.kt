package no.nav.helse.spesialist.api

import io.prometheus.client.Counter
import no.nav.helse.spesialist.api.utbetaling.SaksbehandlerHendelse

//private val overstyringsteller = Counter.build("overstyringer", "Teller antall overstyringer")
//    .labelNames("opplysningstype", "type")
//    .register()

private val tellere = mutableMapOf<String, Counter>()

internal fun tellHendelse(hendelse: SaksbehandlerHendelse) {
    val tellernavn = hendelse.tellernavn()
    tellere.getOrPut(tellernavn) { Counter.build(tellernavn, "Teller antall $tellernavn").register() }.inc()
}