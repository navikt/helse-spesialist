package no.nav.helse.spesialist.api

import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SaksbehandlerHandling
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto

interface Saksbehandlerhåndterer {
    fun <T: SaksbehandlerHandling> håndter(handling: T, saksbehandlerFraApi: SaksbehandlerFraApi)
    fun opprettAbonnement(saksbehandlerFraApi: SaksbehandlerFraApi, personidentifikator: String)
    fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi, sisteSekvensId: Int): List<Opptegnelse>
    fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse>
    fun håndter(godkjenning: GodkjenningDto, behandlingId: UUID, saksbehandlerFraApi: SaksbehandlerFraApi)
    fun håndterTotrinnsvurdering(oppgavereferanse: Long)
}

