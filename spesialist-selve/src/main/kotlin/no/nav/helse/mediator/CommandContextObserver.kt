package no.nav.helse.mediator

internal interface CommandContextObserver : UtgåendeMeldingerObserver {
    fun tilstandEndret(event: KommandokjedeEndretEvent) {}
}
