package no.nav.helse.spesialist.api.saksbehandler

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
}