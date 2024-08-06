package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.graphql.schema.Annullering
import no.nav.helse.spesialist.api.graphql.schema.Avslag
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import java.util.UUID

interface Saksbehandlerhåndterer {
    fun <T : HandlingFraApi> håndter(
        handlingFraApi: T,
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

    fun håndterAvslag(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag,
    )

    fun hentAnnullering(utbetalingId: UUID): Annullering?
}
