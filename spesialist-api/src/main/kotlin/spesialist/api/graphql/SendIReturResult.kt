package no.nav.helse.spesialist.api.graphql

sealed interface SendIReturResult {
    data object Ok : SendIReturResult

    sealed interface Feil : SendIReturResult {
        data class KunneIkkeSendeIRetur(
            val modellfeil: Modellfeil,
        ) : Feil

        data class KunneIkkeLeggePåVent(
            val modellfeil: Modellfeil,
        ) : Feil

        data class KunneIkkeOppretteHistorikkinnslag(
            val exception: Exception,
        ) : Feil
    }
}
