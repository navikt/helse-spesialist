package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.standardfelter
import no.nav.helse.modell.vedtak.Periodetype
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.*

internal class VedtaksperiodeGodkjent(
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val warnings: List<WarningDto>,
    val periodetype: Periodetype,
    val løsning: JsonNode
) {
    internal fun toJson() =
        JsonMessage.newMessage(standardfelter("vedtaksperiode_godkjent", fødselsnummer).apply { this.putAll(mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "warnings" to warnings,
            "periodetype" to periodetype.name,
            "saksbehandlerIdent" to løsning["Godkjenning"]["saksbehandlerIdent"].asText(),
            "saksbehandlerEpost" to løsning["Godkjenning"]["saksbehandlerEpost"].asText(),
            "automatiskBehandling" to løsning["Godkjenning"]["automatiskBehandling"].asBoolean()
        ))}).toJson()
}
