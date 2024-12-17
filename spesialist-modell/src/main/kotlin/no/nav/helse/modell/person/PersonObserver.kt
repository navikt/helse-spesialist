package no.nav.helse.modell.person

import no.nav.helse.modell.hendelse.Sykepengevedtak

interface PersonObserver {
    fun sykepengevedtak(sykepengevedtak: Sykepengevedtak) {}
}
