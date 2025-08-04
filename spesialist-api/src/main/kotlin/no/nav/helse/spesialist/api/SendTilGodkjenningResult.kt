package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil

sealed interface SendTilGodkjenningResult {
    data object Ok : SendTilGodkjenningResult

    sealed interface Feil : SendTilGodkjenningResult {
        data class ManglerVurderingAvVarsler(
            val modellfeil: Modellfeil,
        ) : Feil

        data class KunneIkkeFinnePerioderTilBehandling(
            val e: Exception,
        ) : Feil

        data class KunneIkkeHåndtereBegrunnelse(
            val e: Exception,
        ) : Feil

        data class KunneIkkeSendeTilBeslutter(
            val modellfeil: Modellfeil,
        ) : Feil

        data class UventetFeilVedSendigTilBeslutter(
            val e: Exception,
        ) : Feil

        data class KunneIkkeFjerneFraPåVent(
            val modellfeil: Modellfeil,
        ) : Feil

        data class UventetFeilVedFjernFraPåVent(
            val e: Exception,
        ) : Feil

        data class UventetFeilVedOpprettingAvPeriodehistorikk(
            val e: Exception,
        ) : Feil
    }
}
