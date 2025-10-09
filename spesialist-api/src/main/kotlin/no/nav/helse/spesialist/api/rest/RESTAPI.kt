package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.GetPersonTilkomneInntektskilderHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektEndreHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektFjernHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektGjenopprettHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkomneInntekterHåndterer
import java.util.UUID

val RESTHÅNDTERERE =
    listOf(
        GetAktiveSaksbehandlereHåndterer(),
        GetOppgaverHåndterer(),
        PostOpphevStansHåndterer(),
        GetPersonTilkomneInntektskilderHåndterer(),
        PostTilkomneInntekterHåndterer(),
        PostTilkommenInntektEndreHåndterer(),
        PostTilkommenInntektFjernHåndterer(),
        PostTilkommenInntektGjenopprettHåndterer(),
    )

fun Routing.restRoutes(
    restDelegator: RestDelegator,
    eksponerOpenApi: Boolean,
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
            RESTHÅNDTERERE.forEach {
                when (it) {
                    is GetHåndterer<*, *> ->
                        get(it.urlPath.substringBefore('?'), {
                            response {
                                code(HttpStatusCode.OK) {
                                    body(it.responseBodyType)
                                }
                            }
                        }) {
                            restDelegator.utførGet(call = call, håndterer = it)
                        }

                    is PostHåndterer<*, *, *> ->
                        post(it.urlPath.substringBefore('?'), {
                            request {
                                body(it.requestBodyType)
                            }
                            response {
                                code(HttpStatusCode.OK) {
                                    body(it.responseBodyType)
                                }
                            }
                        }) {
                            restDelegator.utførPost(call = call, håndterer = it)
                        }
                }
            }
        }
    }
}

fun Parameters.getRequired(name: String): String = this[name] ?: throw HttpNotFound("Mangler parameter $name i URL'en")

fun Parameters.getRequiredUUID(name: String): UUID {
    val string = getRequired(name)
    try {
        return UUID.fromString(string)
    } catch (_: IllegalArgumentException) {
        throw HttpNotFound("Parameter $name i URL'en er ikke en UUID")
    }
}
