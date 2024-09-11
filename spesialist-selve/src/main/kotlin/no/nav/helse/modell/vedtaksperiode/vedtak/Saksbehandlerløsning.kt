package no.nav.helse.modell.vedtaksperiode.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDateTime
import java.util.UUID

/**
 * Behandler input til godkjenningsbehov fra saksbehandler som har blitt lagt på rapid-en av API-biten av spesialist.
 */

internal class Saksbehandlerløsning private constructor(
    override val id: UUID,
    val oppgaveId: Long,
    val godkjenningsbehovhendelseId: UUID,
    private val fødselsnummer: String,
    val godkjent: Boolean,
    val ident: String,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
    val saksbehandleroverstyringer: List<UUID>,
    val saksbehandler: Saksbehandler,
    val beslutter: Saksbehandler?,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        oppgaveId = packet["oppgaveId"].asLong(),
        godkjenningsbehovhendelseId = UUID.fromString(packet["hendelseId"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        godkjent = packet["godkjent"].asBoolean(),
        ident = packet["saksbehandlerident"].asText(),
        epostadresse = packet["saksbehandlerepost"].asText(),
        godkjenttidspunkt = packet["godkjenttidspunkt"].asLocalDateTime(),
        årsak = packet["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
        begrunnelser = packet["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
        kommentar = packet["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
        saksbehandler =
            Saksbehandler(
                packet["saksbehandler.ident"].asText(),
                packet["saksbehandler.epostadresse"].asText(),
            ),
        beslutter =
            packet["beslutter"].takeUnless(JsonNode::isMissingOrNull)?.let {
                Saksbehandler(
                    packet["beslutter.ident"].asText(),
                    packet["beslutter.epostadresse"].asText(),
                )
            },
        saksbehandleroverstyringer =
            packet["saksbehandleroverstyringer"].takeUnless(JsonNode::isMissingOrNull)?.map {
                UUID.fromString(it.asText())
            } ?: emptyList(),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        oppgaveId = jsonNode["oppgaveId"].asLong(),
        godkjenningsbehovhendelseId = UUID.fromString(jsonNode["hendelseId"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        godkjent = jsonNode["godkjent"].asBoolean(),
        ident = jsonNode["saksbehandlerident"].asText(),
        epostadresse = jsonNode["saksbehandlerepost"].asText(),
        godkjenttidspunkt = jsonNode["godkjenttidspunkt"].asLocalDateTime(),
        årsak = jsonNode.path("årsak").takeUnless(JsonNode::isMissingOrNull)?.asText(),
        begrunnelser = jsonNode.path("begrunnelser").takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
        kommentar = jsonNode.path("kommentar").takeUnless(JsonNode::isMissingOrNull)?.asText(),
        saksbehandler =
            Saksbehandler(
                jsonNode["saksbehandler"]["ident"].asText(),
                jsonNode["saksbehandler"]["epostadresse"].asText(),
            ),
        beslutter =
            jsonNode.path("beslutter").takeUnless(JsonNode::isMissingOrNull)?.let {
                Saksbehandler(
                    jsonNode["beslutter"]["ident"].asText(),
                    jsonNode["beslutter"]["epostadresse"].asText(),
                )
            },
        saksbehandleroverstyringer =
            jsonNode.path("saksbehandleroverstyringer").takeUnless(JsonNode::isMissingOrNull)?.map {
                UUID.fromString(it.asText())
            } ?: emptyList(),
        json = jsonNode.toString(),
    )

    internal data class Saksbehandler(
        val ident: String,
        val epostadresse: String,
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        kommandostarter { løsGodkjenningsbehov(this@Saksbehandlerløsning, person) }
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson() = json
}
