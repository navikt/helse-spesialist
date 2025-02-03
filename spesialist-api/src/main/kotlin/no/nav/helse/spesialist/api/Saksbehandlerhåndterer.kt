package no.nav.helse.spesialist.api

import no.nav.helse.modell.Annullering
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutation
import no.nav.helse.spesialist.api.graphql.schema.ApiAvslag
import no.nav.helse.spesialist.api.graphql.schema.ApiOpptegnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakBegrunnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import java.util.UUID

sealed interface SendTilGodkjenningResult {
    data object Ok : SendTilGodkjenningResult

    sealed interface Feil : SendTilGodkjenningResult {
        data class ManglerVurderingAvVarsler(val modellfeil: Modellfeil) : Feil

        data class KunneIkkeFinnePerioderTilBehandling(val e: Exception) : Feil

        data class KunneIkkeHåndtereBegrunnelse(val e: Exception) : Feil

        data class KunneIkkeSendeTilBeslutter(val modellfeil: Modellfeil) : Feil

        data class UventetFeilVedSendigTilBeslutter(val e: Exception) : Feil

        data class KunneIkkeFjerneFraPåVent(val modellfeil: Modellfeil) : Feil

        data class UventetFeilVedFjernFraPåVent(val e: Exception) : Feil

        data class UventetFeilVedOpprettingAvPeriodehistorikk(val e: Exception) : Feil
    }
}

interface Saksbehandlerhåndterer {
    fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag?,
    ): VedtakMutation.VedtakResultat

    fun vedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
        utfall: ApiVedtakUtfall,
        begrunnelse: String?,
    ): VedtakMutation.VedtakResultat

    fun infotrygdVedtak(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        oppgavereferanse: Long,
        godkjent: Boolean,
    ): VedtakMutation.VedtakResultat

    fun håndter(
        handlingFraApi: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    )

    fun påVent(
        handling: ApiPaVentRequest,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    )

    fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    )

    fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<ApiOpptegnelse>

    fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<ApiOpptegnelse>

    // TODO: Ser ut til å være død kode, brukes bare av tester
    fun håndter(
        godkjenning: GodkjenningDto,
        behandlingId: UUID,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    )

    fun håndterTotrinnsvurdering(
        oppgavereferanse: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        utfall: ApiVedtakUtfall,
        begrunnelse: String?,
    ): SendTilGodkjenningResult

    fun hentAvslag(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<ApiAvslag>

    fun hentVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<ApiVedtakBegrunnelse>

    fun håndterAvslag(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag,
    )

    fun håndterVedtakBegrunnelse(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        utfall: ApiVedtakUtfall,
        begrunnelse: String?,
    )

    fun hentAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?
}
