package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.GetTilkomneInntektskilderHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektEndreHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektFjernHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektGjenopprettHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektLeggTilHåndterer
import java.util.UUID

fun Routing.restRoutes(restDelegator: RestDelegator) {
    route("api") {
        authenticate("oidc") {
            listOf(GetTilkomneInntektskilderHåndterer()).forEach {
                get(it.urlPath) {
                    restDelegator.utførGet(
                        call = call,
                        håndterer = it,
                        parameterTolkning = it::extractParametre,
                    )
                }
            }
            post("opphevstans") {
                restDelegator.utførPost(
                    call = call,
                    håndterer = PostOpphevStansHåndterer(),
                    parameterTolkning = { },
                )
            }
            route("tilkomne-inntekter") {
                post {
                    restDelegator.utførPost(
                        call = call,
                        håndterer = PostTilkommenInntektLeggTilHåndterer(),
                        parameterTolkning = { },
                    )
                }
                route("{tilkommenInntektId}") {
                    post("endre") {
                        restDelegator.utførPost(
                            call = call,
                            håndterer = PostTilkommenInntektEndreHåndterer(),
                            parameterTolkning = { parametre ->
                                PostTilkommenInntektEndreHåndterer.URLParametre(
                                    tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                                )
                            },
                        )
                    }
                    post("fjern") {
                        restDelegator.utførPost(
                            call = call,
                            håndterer = PostTilkommenInntektFjernHåndterer(),
                            parameterTolkning = { parametre ->
                                PostTilkommenInntektFjernHåndterer.URLParametre(
                                    tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                                )
                            },
                        )
                    }
                    post("gjenopprett") {
                        restDelegator.utførPost(
                            call = call,
                            håndterer = PostTilkommenInntektGjenopprettHåndterer(),
                            parameterTolkning = { parametre ->
                                PostTilkommenInntektGjenopprettHåndterer.URLParametre(
                                    tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

fun Parameters.getRequired(name: String): String = this[name] ?: throw HttpNotFound("Mangler parameter $name i URL'en")

private fun Parameters.getRequiredUUID(name: String): UUID {
    val string = getRequired(name)
    try {
        return UUID.fromString(string)
    } catch (_: IllegalArgumentException) {
        throw HttpNotFound("Parameter $name i URL'en er ikke en UUID")
    }
}
