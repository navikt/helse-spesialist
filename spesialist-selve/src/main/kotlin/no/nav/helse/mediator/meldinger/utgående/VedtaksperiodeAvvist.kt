package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.standardfelter
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.*

internal class VedtaksperiodeAvvist(
    val vedtaksperiodeId: UUID,
    val fødselsnummer: String,
    val warnings: List<WarningDto>,
    val periodetype: Periodetype?,
    val løsning: JsonNode
) {
    internal fun toJson() =
        JsonMessage.newMessage(standardfelter("vedtaksperiode_avvist", fødselsnummer)
            .apply {
                putAll(
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId,
                        "warnings" to warnings,
                        "saksbehandlerIdent" to løsning["Godkjenning"]["saksbehandlerIdent"].asText(),
                        "saksbehandlerEpost" to løsning["Godkjenning"]["saksbehandlerEpost"].asText(),
                        "automatiskBehandling" to løsning["Godkjenning"]["automatiskBehandling"].asBoolean(),
                        "årsak" to løsning["Godkjenning"]["årsak"].asText(),
                        "begrunnelser" to løsning["Godkjenning"]["begrunnelser"].asText(),
                        "kommentar" to løsning["Godkjenning"]["kommentar"].asText()
                    )
                )
            }
            .apply { if (periodetype != null) put("periodetype", periodetype.name) }
        ).toJson()

}
