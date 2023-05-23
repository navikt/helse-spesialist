package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.JsonMessage

internal class VedtaksperiodeAvvist(
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val periodetype: Periodetype?,
    val løsning: JsonNode
) {
    internal fun toJson() =
        JsonMessage.newMessage("vedtaksperiode_avvist", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "saksbehandlerIdent" to løsning["Godkjenning"]["saksbehandlerIdent"].asText(),
            "saksbehandlerEpost" to løsning["Godkjenning"]["saksbehandlerEpost"].asText(),
            "automatiskBehandling" to løsning["Godkjenning"]["automatiskBehandling"].asBoolean(),
            "årsak" to løsning["Godkjenning"]["årsak"].asText(),
            "begrunnelser" to løsning["Godkjenning"]["begrunnelser"].map(JsonNode::asText),
            "kommentar" to løsning["Godkjenning"]["kommentar"].asText()
            ).apply {
                compute("periodetype") { _, _ -> periodetype?.name }
            }
        ).toJson()

}
