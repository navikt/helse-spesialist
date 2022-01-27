package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.mediator.standardfelter
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.saksbehandler.SaksbehandlerDto
import java.time.LocalDate
import java.util.*

internal fun Route.overstyringApi(hendelseMediator: HendelseMediator) {
    post("/api/overstyr/dager") {
        val overstyring = call.receive<OverstyrTidslinjeDTO>()

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
                OverstyrTidslinjeKafkaDto.Dag(
                    dato = it.dato,
                    type = enumValueOf(it.type),
                    grad = it.grad
                )
            }
        )
        withContext(Dispatchers.IO) { hendelseMediator.håndter(message) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/overstyr/inntekt") {
        val overstyring = call.receive<OverstyrInntektDTO>()

        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        val message = OverstyrInntektKafkaDto(
            organisasjonsnummer = overstyring.organisasjonsnummer,
            fødselsnummer = overstyring.fødselsnummer,
            aktørId = overstyring.aktørId,
            saksbehandler = saksbehandler.toDto(),
            begrunnelse = overstyring.begrunnelse,
            forklaring = overstyring.forklaring,
            månedligInntekt = overstyring.månedligInntekt,
            skjæringstidspunkt = overstyring.skjæringstidspunkt
        )
        withContext(Dispatchers.IO) { hendelseMediator.håndter(message) }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/overstyr/arbeidsforhold") {
        if (Toggle.OverstyrArbeidsforhold.enabled) {
            val overstyring = call.receive<OverstyrArbeidsforholdDto>()

            val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

            val message = OverstyrArbeidsforholdKafkaDto(
                saksbehandler = saksbehandler.toDto(),
                organisasjonsnummer = overstyring.organisasjonsnummer,
                fødselsnummer = overstyring.fødselsnummer,
                aktørId = overstyring.aktørId,
                skjæringstidspunkt = overstyring.skjæringstidspunkt,
                overstyrteArbeidsforhold = overstyring.overstyrteArbeidsforhold
            )
            withContext(Dispatchers.IO) { hendelseMediator.håndter(message) }
            call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
        } else {
            call.respond(HttpStatusCode.NotImplemented, "Featuren er skrudd av")
        }
    }
}


@JsonIgnoreProperties
class OverstyrTidslinjeDTO(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyringdagDTO>
) {
    class OverstyringdagDTO(
        val dato: LocalDate,
        val type: String,
        val grad: Int?
    )
}

data class OverstyrTidslinjeKafkaDto(
    val saksbehandlerEpost: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<Dag>
) {
    data class Dag(
        val dato: LocalDate,
        val type: Type,
        val grad: Int?
    ) {
        enum class Type { Sykedag, Feriedag, Egenmeldingsdag, Permisjonsdag }
    }
}

data class OverstyrInntektDTO(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val forklaring: String,
    val månedligInntekt: Double,
    val skjæringstidspunkt: LocalDate
)

data class OverstyrInntektKafkaDto(
    val saksbehandler: SaksbehandlerDto,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val forklaring: String,
    val månedligInntekt: Double,
    val skjæringstidspunkt: LocalDate
) {
    fun somKafkaMessage() = JsonMessage.newMessage(
        standardfelter("overstyr_inntekt", fødselsnummer).apply {
            put("aktørId", aktørId)
            put("organisasjonsnummer", organisasjonsnummer)
            put("begrunnelse", begrunnelse)
            put("forklaring", forklaring)
            put("saksbehandlerOid", saksbehandler.oid)
            put("saksbehandlerNavn", saksbehandler.navn)
            put("saksbehandlerIdent", saksbehandler.ident)
            put("saksbehandlerEpost", saksbehandler.epost)
            put("månedligInntekt", månedligInntekt)
            put("skjæringstidspunkt", skjæringstidspunkt)
        }
    )
}

data class OverstyrArbeidsforholdDto(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>
) {
    data class ArbeidsforholdOverstyrt(
        val orgnummer: String,
        val erAktivt: Boolean,
        val begrunnelse: String,
        val forklaring: String
    )
}

data class OverstyrArbeidsforholdKafkaDto(
    val saksbehandler: SaksbehandlerDto,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>
) {
    fun somKafkaMessage() = JsonMessage.newMessage(
        standardfelter("overstyr_arbeidsforhold", fødselsnummer).apply {
            put("aktørId", aktørId)
            put("organisasjonsnummer", organisasjonsnummer)
            put("saksbehandlerOid", saksbehandler.oid)
            put("saksbehandlerNavn", saksbehandler.navn)
            put("saksbehandlerIdent", saksbehandler.ident)
            put("saksbehandlerEpost", saksbehandler.epost)
            put("skjæringstidspunkt", skjæringstidspunkt)
            put("overstyrteArbeidsforhold", overstyrteArbeidsforhold)
        }
    )
}
