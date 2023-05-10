package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdKafkaDto
import no.nav.helse.spesialist.api.overstyring.OverstyrInntektOgRefusjonDto
import no.nav.helse.spesialist.api.overstyring.OverstyrInntektOgRefusjonKafkaDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.OverstyrTidslinjeKafkaDto
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler

fun Route.overstyringApi(saksbehandlerMediator: SaksbehandlerMediator) {
    post("/api/overstyr/dager") {
        val overstyring = call.receive<OverstyrTidslinjeDto>()

        val accessToken = requireNotNull(call.principal<JWTPrincipal>())
        val oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())
        val epostadresse = accessToken.payload.getClaim("preferred_username").asString()
        val saksbehandlerNavn = accessToken.payload.getClaim("name").asString()
        val saksbehandlerIdent = accessToken.payload.getClaim("NAVident").asString()

        val message = OverstyrTidslinjeKafkaDto(
            saksbehandlerEpost = epostadresse,
            saksbehandlerOid = oid,
            saksbehandlerNavn = saksbehandlerNavn,
            saksbehandlerIdent = saksbehandlerIdent,
            organisasjonsnummer = overstyring.organisasjonsnummer,
            fødselsnummer = overstyring.fødselsnummer,
            aktørId = overstyring.aktørId,
            begrunnelse = overstyring.begrunnelse,
            dager = overstyring.dager.map {
                OverstyrTidslinjeKafkaDto.OverstyrDagKafkaDto(
                    dato = it.dato,
                    type = enumValueOf(it.type),
                    fraType = enumValueOf(it.fraType),
                    grad = it.grad,
                    fraGrad = it.fraGrad
                )
            }
        )
        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(message) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/overstyr/inntektogrefusjon") {
        val overstyring = call.receive<OverstyrInntektOgRefusjonDto>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        val message = OverstyrInntektOgRefusjonKafkaDto(
            fødselsnummer = overstyring.fødselsnummer,
            aktørId = overstyring.aktørId,
            skjæringstidspunkt = overstyring.skjæringstidspunkt,
            saksbehandler = saksbehandler.toDto(),
            arbeidsgivere = overstyring.arbeidsgivere,
        )
        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(message) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/overstyr/arbeidsforhold") {
        val overstyring = call.receive<OverstyrArbeidsforholdDto>()

        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        val message = OverstyrArbeidsforholdKafkaDto(
            saksbehandler = saksbehandler.toDto(),
            fødselsnummer = overstyring.fødselsnummer,
            aktørId = overstyring.aktørId,
            skjæringstidspunkt = overstyring.skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyring.overstyrteArbeidsforhold
        )
        withContext(Dispatchers.IO) { saksbehandlerMediator.håndter(message) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}