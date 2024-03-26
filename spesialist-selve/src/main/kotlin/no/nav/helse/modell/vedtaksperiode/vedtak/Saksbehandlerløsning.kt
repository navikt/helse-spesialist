package no.nav.helse.modell.vedtaksperiode.vedtak

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.PersonmeldingOld
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull

/**
 * Behandler input til godkjenningsbehov fra saksbehandler som har blitt lagt på rapid-en av API-biten av spesialist.
 */


internal class Saksbehandlerløsning private constructor(
    override val id: UUID,
    val behandlingId: UUID,
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
) : PersonmeldingOld {

    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        behandlingId = UUID.fromString(packet["behandlingId"].asText()),
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
        saksbehandler = Saksbehandler(
            packet["saksbehandler.ident"].asText(),
            packet["saksbehandler.epostadresse"].asText()
        ),
        beslutter = packet["beslutter"].takeUnless(JsonNode::isMissingOrNull)?.let {
            Saksbehandler(
                packet["beslutter.ident"].asText(),
                packet["beslutter.epostadresse"].asText()
            )
        },
        saksbehandleroverstyringer = packet["saksbehandleroverstyringer"].takeUnless(JsonNode::isMissingOrNull)?.map { UUID.fromString(it.asText()) } ?: emptyList(),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode): this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        behandlingId = UUID.fromString(jsonNode["behandlingId"].asText()),
        oppgaveId = jsonNode["oppgaveId"].asLong(),
        godkjenningsbehovhendelseId = UUID.fromString(jsonNode["hendelseId"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        godkjent = jsonNode["godkjent"].asBoolean(),
        ident = jsonNode["saksbehandlerident"].asText(),
        epostadresse = jsonNode["saksbehandlerepost"].asText(),
        godkjenttidspunkt = jsonNode["godkjenttidspunkt"].asLocalDateTime(),
        årsak = jsonNode["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
        begrunnelser = jsonNode["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
        kommentar = jsonNode["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
        saksbehandler = Saksbehandler(
            jsonNode["saksbehandler.ident"].asText(),
            jsonNode["saksbehandler.epostadresse"].asText()
        ),
        beslutter = jsonNode["beslutter"].takeUnless(JsonNode::isMissingOrNull)?.let {
            Saksbehandler(
                jsonNode["beslutter.ident"].asText(),
                jsonNode["beslutter.epostadresse"].asText()
            )
        },
        saksbehandleroverstyringer = jsonNode["saksbehandleroverstyringer"].takeUnless(JsonNode::isMissingOrNull)?.map { UUID.fromString(it.asText()) } ?: emptyList(),
        json = jsonNode.toString(),
    )

    internal data class Saksbehandler(
        val ident: String,
        val epostadresse: String
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}
