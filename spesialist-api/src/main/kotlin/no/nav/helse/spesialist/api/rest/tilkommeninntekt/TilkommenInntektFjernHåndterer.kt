package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RestHandler
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import java.util.UUID

class TilkommenInntektFjernHåndterer(
    private val handler: RestHandler,
    private val tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) : PostHåndterer<TilkommenInntektFjernHåndterer.URLParametre, TilkommenInntektFjernHåndterer.RequestBody, HttpStatusCode> {
    data class URLParametre(
        val tilkommenInntektId: UUID,
    )

    data class RequestBody(
        val notatTilBeslutter: String,
    )

    override fun håndter(
        urlParametre: URLParametre,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): HttpStatusCode {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(urlParametre.tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId ${urlParametre.tilkommenInntektId}")

        handler.kontrollerTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        tilkommenInntektMutationHandler.fjernTilkommenInntekt(
            tilkommenInntekt = tilkommenInntekt,
            notatTilBeslutter = requestBody.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )

        return HttpStatusCode.NoContent
    }
}
