package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.JsonMessage

internal class VedtaksperiodeGodkjent(
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val warnings: List<WarningDto>,
    val periodetype: Periodetype,
    val løsning: JsonNode
) {
    internal fun toJson() =
        JsonMessage.newMessage("vedtaksperiode_godkjent", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "warnings" to warnings,
            "periodetype" to periodetype.name,
            "saksbehandlerIdent" to løsning["Godkjenning"]["saksbehandlerIdent"].asText(),
            "saksbehandlerEpost" to løsning["Godkjenning"]["saksbehandlerEpost"].asText(),
            "automatiskBehandling" to løsning["Godkjenning"]["automatiskBehandling"].asBoolean()
        )).toJson()
}
