package no.nav.helse.modell.person

import no.nav.helse.modell.vedtak.Sykepengevedtak

internal interface PersonObserver {
    fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {}
}
