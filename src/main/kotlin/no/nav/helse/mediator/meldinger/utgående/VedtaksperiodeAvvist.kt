package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.standardfelter
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.*

internal class VedtaksperiodeAvvist(
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val warnings: List<Warning>,
    val periodetype: Saksbehandleroppgavetype,
    val løsning: JsonNode
) {
    internal fun toJson() =
        JsonMessage.newMessage(standardfelter("vedtaksperiode_avvist", fødselsnummer).apply { this.putAll(mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "warnings" to warnings,
            "periodetype" to periodetype.name,
            "saksbehandlerIdent" to løsning["Godkjenning"]["saksbehandlerIdent"].asText(),
            "saksbehandlerEpost" to løsning["Godkjenning"]["saksbehandlerEpost"].asText(),
            "automatiskBehandling" to løsning["Godkjenning"]["automatisk"].asBoolean(),
            "årsak" to løsning["Godkjenning"]["årsak"].asText(),
            "begrunnelser" to løsning["Godkjenning"]["begrunnelser"].asText(),
            "kommentar" to løsning["Godkjenning"]["kommentar"].asText()
        ))}).toJson()
}
