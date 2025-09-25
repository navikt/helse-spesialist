package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.LeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RestHandler
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class TilkommenInntektLeggTilHåndterer(
    private val handler: RestHandler,
    private val tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) : PostHåndterer<Unit, TilkommenInntektLeggTilHåndterer.RequestBody, LeggTilTilkommenInntektResponse> {
    data class RequestBody(
        val fodselsnummer: String,
        val verdier: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    override fun håndter(
        urlParametre: Unit,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): LeggTilTilkommenInntektResponse {
        handler.kontrollerTilgangTilPerson(
            fødselsnummer = requestBody.fodselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        return tilkommenInntektMutationHandler.leggTilTilkommenInntekt(
            fodselsnummer = requestBody.fodselsnummer,
            verdier = requestBody.verdier,
            notatTilBeslutter = requestBody.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )
    }
}
