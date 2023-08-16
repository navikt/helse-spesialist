package no.nav.helse.modell.sykefraværstilfelle

import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengevedtak

internal interface SykefraværstilfelleObserver {
    fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {}
}