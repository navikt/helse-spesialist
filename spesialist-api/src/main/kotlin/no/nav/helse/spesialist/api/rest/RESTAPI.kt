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
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.rest.behandlinger.PostForkastingBehandler
import no.nav.helse.spesialist.api.rest.behandlinger.PostVedtakBehandler
import no.nav.helse.spesialist.api.rest.opptegnelsesekvensnummer.GetOpptegnelseSekvensnummerSisteBehandler
import no.nav.helse.spesialist.api.rest.personer.GetKrrRegistrertStatusForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.GetKrrStatusForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.GetOpptegnelserForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.GetTilkomneInntektskilderForPersonBehandler
import no.nav.helse.spesialist.api.rest.personer.PostPersonSokBehandler
import no.nav.helse.spesialist.api.rest.personer.dokumenter.GetInntektsmeldingBehandler
import no.nav.helse.spesialist.api.rest.personer.dokumenter.GetSoknadBehandler
import no.nav.helse.spesialist.api.rest.personer.vurderinger.PostArbeidstidsvurderingBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.PatchTilkommenInntektBehandler
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.PostTilkomneInntekterBehandler
import no.nav.helse.spesialist.api.rest.varsler.DeleteVarselvurderingBehandler
import no.nav.helse.spesialist.api.rest.varsler.GetVarselBehandler
import no.nav.helse.spesialist.api.rest.varsler.PutVarselvurderingBehandler
import no.nav.helse.spesialist.api.rest.vedtaksperioder.PostVedtaksperiodeAnnullerBehandler
import no.nav.helse.spesialist.api.rest.vedtaksperioder.notater.PostNotatBehandler
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter

fun Routing.restRoutes(
    restAdapter: RestAdapter,
    configuration: ApiModule.Configuration,
    dokumentMediator: DokumentMediator,
    environmentToggles: EnvironmentToggles,
    krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
) {
    route("/api") {
        if (configuration.eksponerOpenApi) {
            route("/openapi.json") {
                openApi()
            }
            route("swagger") {
                swaggerUI("../openapi.json")
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
            patch(PatchTilkommenInntektBehandler(), restAdapter)

            post(PostVedtakBehandler(environmentToggles), restAdapter)
            post(PostForkastingBehandler(), restAdapter)

            post(PostVedtaksperiodeAnnullerBehandler(), restAdapter)

            post(PostArbeidstidsvurderingBehandler(), restAdapter)

            get(GetVarselBehandler(), restAdapter)
            put(PutVarselvurderingBehandler(), restAdapter)
            delete(DeleteVarselvurderingBehandler(), restAdapter)

            post(PostNotatBehandler(), restAdapter)

            get(GetOpptegnelseSekvensnummerSisteBehandler(), restAdapter)
            get(GetOpptegnelserForPersonBehandler(), restAdapter)

            post(PostPersonSokBehandler(), restAdapter)

            get(GetKrrStatusForPersonBehandler(krrRegistrertStatusHenter), restAdapter)

            get(GetKrrRegistrertStatusForPersonBehandler(krrRegistrertStatusHenter), restAdapter)
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
