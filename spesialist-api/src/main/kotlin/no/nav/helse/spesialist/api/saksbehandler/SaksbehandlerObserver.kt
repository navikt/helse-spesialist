package no.nav.helse.spesialist.api.saksbehandler

import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinje

internal interface SaksbehandlerObserver {
    fun annulleringEvent(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        saksbehandler: Map<String, Any>,
        fagsystemId: String,
        begrunnelser: List<String>,
        kommentar: String?
    ) {}

    fun overstyrTidslinjeEvent(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        saksbehandler: Map<String, Any>,
        begrunnelse: String,
        dager: List<OverstyrTidslinje.Overstyringdag>
    ) {}
}