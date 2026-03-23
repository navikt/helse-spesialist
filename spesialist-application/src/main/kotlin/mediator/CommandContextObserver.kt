package no.nav.helse.mediator

interface CommandContextObserver : UtgåendeMeldingerObserver {
    fun tilstandEndret(event: KommandokjedeEndretEvent) {}
}
