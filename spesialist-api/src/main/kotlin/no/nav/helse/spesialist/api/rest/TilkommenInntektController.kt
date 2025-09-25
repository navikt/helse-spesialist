package no.nav.helse.spesialist.api.rest

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.rest.RestHandler.Companion.getRequired
import no.nav.helse.spesialist.api.rest.RestHandler.Companion.getRequiredUUID
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.GetTilkomneInntektskilderHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektEndreHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektFjernHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektGjenopprettHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PostTilkommenInntektLeggTilHåndterer

class TilkommenInntektController(
    private val handler: RestHandler,
    private val tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) {
    fun addToRoute(route: Route) {
        route.post("tidligere-mutations/tilkommen-inntekt/legg-til") {
            handler.håndterPost(
                call = call,
                håndterer = PostTilkommenInntektLeggTilHåndterer(handler, tilkommenInntektMutationHandler),
                parameterTolkning = { },
            )
        }
        route.route("tilkomne-inntekter") {
            post("legg-til") {
                handler.håndterPost(
                    call = call,
                    håndterer = PostTilkommenInntektLeggTilHåndterer(handler, tilkommenInntektMutationHandler),
                    parameterTolkning = { },
                )
            }
            route("{tilkommenInntektId}") {
                post("endre") {
                    handler.håndterPost(
                        call = call,
                        håndterer = PostTilkommenInntektEndreHåndterer(handler, tilkommenInntektMutationHandler),
                        parameterTolkning = { parametre ->
                            PostTilkommenInntektEndreHåndterer.URLParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            )
                        },
                    )
                }
                post("fjern") {
                    handler.håndterPost(
                        call = call,
                        håndterer = PostTilkommenInntektFjernHåndterer(handler, tilkommenInntektMutationHandler),
                        parameterTolkning = { parametre ->
                            PostTilkommenInntektFjernHåndterer.URLParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            )
                        },
                    )
                }
                post("gjenopprett") {
                    handler.håndterPost(
                        call = call,
                        håndterer = PostTilkommenInntektGjenopprettHåndterer(handler, tilkommenInntektMutationHandler),
                        parameterTolkning = { parametre ->
                            PostTilkommenInntektGjenopprettHåndterer.URLParametre(
                                tilkommenInntektId = parametre.getRequiredUUID("tilkommenInntektId"),
                            )
                        },
                    )
                }
            }
        }
        route.route("personer/{aktørId}/tilkomne-inntektskilder") {
            get {
                handler.håndterGet(
                    call = call,
                    håndterer = GetTilkomneInntektskilderHåndterer(handler),
                    parameterTolkning = { parametre ->
                        GetTilkomneInntektskilderHåndterer.URLParametre(
                            aktørId = parametre.getRequired("aktørId"),
                        )
                    },
                )
            }
        }
    }
}
