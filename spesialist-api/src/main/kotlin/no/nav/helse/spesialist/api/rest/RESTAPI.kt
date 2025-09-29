package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.PersonTilkomneInntektskilderGetHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektEndrePostHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektFjernPostHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkommenInntektGjenopprettPostHåndterer
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.TilkomneInntekterPostHåndterer
import java.util.UUID

val RESTHÅNDTERERE =
    listOf(
        PersonTilkomneInntektskilderGetHåndterer(),
        OpphevStansPostHåndterer(),
        TilkomneInntekterPostHåndterer(),
        TilkommenInntektEndrePostHåndterer(),
        TilkommenInntektFjernPostHåndterer(),
        TilkommenInntektGjenopprettPostHåndterer(),
        AktiveSaksbehandlereGetHåndterer(),
    )

fun Routing.restRoutes(restDelegator: RestDelegator) {
    route("api") {
        authenticate("oidc") {
            RESTHÅNDTERERE.forEach {
                when (it) {
                    is GetHåndterer<*, *> ->
                        get(it.urlPath) {
                            restDelegator.utførGet(call = call, håndterer = it)
                        }

                    is PostHåndterer<*, *, *> ->
                        post(it.urlPath) {
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
