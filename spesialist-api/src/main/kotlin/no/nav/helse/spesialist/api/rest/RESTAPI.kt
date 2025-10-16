package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.resources.delete
import io.github.smiley4.ktoropenapi.resources.get
import io.github.smiley4.ktoropenapi.resources.patch
import io.github.smiley4.ktoropenapi.resources.post
import io.github.smiley4.ktoropenapi.resources.put
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.rest.dokument.GetSøknadBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.GetTilkomneInntektskilderForPersonBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektEndreBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektFjernBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektGjenopprettBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkomneInntekterBehandler

fun Routing.restRoutes(
    restAdapter: RestAdapter,
    eksponerOpenApi: Boolean,
    dokumenthåndterer: Dokumenthåndterer,
) {
    route("/api") {
        if (eksponerOpenApi) {
            route("/openapi.json") {
                openApi()
            }
            route("swagger") {
                swaggerUI("/api/openapi.json")
            }
        }
        authenticate("oidc") {
            get(GetAktiveSaksbehandlereBehandler(), restAdapter)

            get(GetOppgaverBehandler(), restAdapter)

            post(PostOpphevStansBehandler(), restAdapter)

            get(GetSøknadBehandler(dokumenthåndterer = dokumenthåndterer), restAdapter)

            get(GetTilkomneInntektskilderForPersonBehandler(), restAdapter)
            post(PostTilkomneInntekterBehandler(), restAdapter)
            post(PostTilkommenInntektEndreBehandler(), restAdapter)
            post(PostTilkommenInntektFjernBehandler(), restAdapter)
            post(PostTilkommenInntektGjenopprettBehandler(), restAdapter)
        }
    }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified RESPONSE : Any> Route.delete(
    behandler: DeleteBehandler<RESOURCE, RESPONSE>,
    adapter: RestAdapter,
) {
    delete<RESOURCE>(behandler::openApi) { resource -> adapter.behandle(resource, call, behandler) }
}

private inline fun <reified RESOURCE : Any, reified RESPONSE : Any> Route.get(
    behandler: GetBehandler<RESOURCE, RESPONSE>,
    adapter: RestAdapter,
) {
    get<RESOURCE>(behandler::openApi) { resource -> adapter.behandle(resource, call, behandler) }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any> Route.patch(
    behandler: PatchBehandler<RESOURCE, REQUEST, RESPONSE>,
    adapter: RestAdapter,
) {
    patch<RESOURCE>(behandler::openApi) { resource -> adapter.behandle(resource, call, behandler) }
}

private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any> Route.post(
    behandler: PostBehandler<RESOURCE, REQUEST, RESPONSE>,
    adapter: RestAdapter,
) {
    post<RESOURCE>(behandler::openApi) { resource -> adapter.behandle(resource, call, behandler) }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any> Route.put(
    behandler: PutBehandler<RESOURCE, REQUEST, RESPONSE>,
    adapter: RestAdapter,
) {
    put<RESOURCE>(behandler::openApi) { resource -> adapter.behandle(resource, call, behandler) }
}
