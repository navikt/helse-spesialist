package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutation
import no.nav.helse.spesialist.api.graphql.mutation.VedtakUtfall
import no.nav.helse.spesialist.api.graphql.schema.Annullering
import no.nav.helse.spesialist.api.graphql.schema.Avslag
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.PaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.VedtakBegrunnelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import java.util.UUID

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
        utfall: VedtakUtfall,
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
        handling: PaVentRequest,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    )

    fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    )

    fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<Opptegnelse>

    fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse>

    // TODO: Ser ut til å være død kode, brukes bare av tester
    fun håndter(
        godkjenning: GodkjenningDto,
        behandlingId: UUID,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    )

    fun håndterTotrinnsvurdering(oppgavereferanse: Long)

    fun hentAvslag(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Avslag>

    fun hentVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<VedtakBegrunnelse>

    fun håndterAvslag(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag,
    )

    fun håndterVedtakBegrunnelse(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        utfall: VedtakUtfall,
        begrunnelse: String?,
    )

    fun hentAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?
}
