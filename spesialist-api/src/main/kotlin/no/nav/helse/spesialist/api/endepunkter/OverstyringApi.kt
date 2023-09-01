package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandling

fun Route.overstyringApi(saksbehandlerMediator: SaksbehandlerMediator) {
    post("/api/overstyr/dager") {
        val overstyring = call.receive<OverstyrTidslinjeHandling>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(overstyring, saksbehandler) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/overstyr/inntektogrefusjon") {
        val overstyring = call.receive<OverstyrInntektOgRefusjonHandling>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(overstyring, saksbehandler) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/overstyr/arbeidsforhold") {
        val overstyring = call.receive<OverstyrArbeidsforholdHandling>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(overstyring, saksbehandler) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/skjonnsfastsett/sykepengegrunnlag") {
        val overstyring = call.receive<SkjønnsfastsettSykepengegrunnlagHandling>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(overstyring, saksbehandler) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}