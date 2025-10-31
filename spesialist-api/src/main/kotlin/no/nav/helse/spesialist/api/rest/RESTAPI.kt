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
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.spesialist.api.rest.dokument.GetInntektsmeldingBehandler
import no.nav.helse.spesialist.api.rest.dokument.GetSoknadBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.GetTilkomneInntektskilderForPersonBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektEndreBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektFjernBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektGjenopprettBehandler
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkomneInntekterBehandler

fun Routing.restRoutes(
    restAdapter: RestAdapter,
    eksponerOpenApi: Boolean,
    dokumentMediator: DokumentMediator,
    environmentToggles: EnvironmentToggles,
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

            get(GetSoknadBehandler(dokumentMediator = dokumentMediator), restAdapter)
            get(GetInntektsmeldingBehandler(dokumentMediator = dokumentMediator), restAdapter)

            get(GetTilkomneInntektskilderForPersonBehandler(), restAdapter)
            post(PostTilkomneInntekterBehandler(), restAdapter)
            post(PostTilkommenInntektEndreBehandler(), restAdapter)
            post(PostTilkommenInntektFjernBehandler(), restAdapter)
            post(PostTilkommenInntektGjenopprettBehandler(), restAdapter)

            post(PostFattVedtakBehandler(environmentToggles), restAdapter)
        }
    }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.delete(
    behandler: DeleteBehandler<RESOURCE, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    delete<RESOURCE>({ behandler.openApiUtenRequestBody<RESPONSE, ERROR>(this) }) { resource -> adapter.behandle(resource, call, behandler) }
}

private inline fun <reified RESOURCE : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.get(
    behandler: GetBehandler<RESOURCE, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    get<RESOURCE>({ behandler.openApiUtenRequestBody<RESPONSE, ERROR>(this) }) { resource -> adapter.behandle(resource, call, behandler) }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.patch(
    behandler: PatchBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    patch<RESOURCE>({ behandler.openApiMedRequestBody<REQUEST, RESPONSE, ERROR>(this) }) { resource -> adapter.behandle(resource, call, behandler) }
}

private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.post(
    behandler: PostBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    post<RESOURCE>({ behandler.openApiMedRequestBody<REQUEST, RESPONSE, ERROR>(this) }) { resource -> adapter.behandle(resource, call, behandler) }
}

@Suppress("unused")
private inline fun <reified RESOURCE : Any, reified REQUEST : Any, reified RESPONSE : Any, reified ERROR : ApiErrorCode> Route.put(
    behandler: PutBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>,
    adapter: RestAdapter,
) {
    put<RESOURCE>({ behandler.openApiMedRequestBody<REQUEST, RESPONSE, ERROR>(this) }) { resource -> adapter.behandle(resource, call, behandler) }
}
