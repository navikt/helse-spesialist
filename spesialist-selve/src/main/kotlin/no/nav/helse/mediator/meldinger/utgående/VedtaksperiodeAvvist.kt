package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class VedtaksperiodeAvvist private constructor(
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID?,
    val fødselsnummer: String,
    val periodetype: Periodetype?,
    val saksbehandler: Saksbehandlerløsning.Saksbehandler,
    val automatiskBehandlet: Boolean,
    val løsning: JsonNode,
) {
    internal fun toJson() =
        JsonMessage.newMessage(
            "vedtaksperiode_avvist",
            buildMap {
                put("fødselsnummer", fødselsnummer)
                put("vedtaksperiodeId", vedtaksperiodeId)
                put("saksbehandlerIdent", saksbehandler.ident)
                put("saksbehandlerEpost", saksbehandler.epostadresse)
                put("saksbehandler", saksbehandler.apply { mapOf("ident" to ident, "epostadresse" to epostadresse) })
                put("automatiskBehandling", automatiskBehandlet)
                put("årsak", løsning["Godkjenning"]["årsak"].asText())
                put("begrunnelser", løsning["Godkjenning"]["begrunnelser"].map(JsonNode::asText))
                put("kommentar", løsning["Godkjenning"]["kommentar"].asText())
                periodetype?.name?.let { put("periodetype", it) }
                spleisBehandlingId?.let { put("behandlingId", it) }
            },
        ).toJson()

    internal companion object {
        fun manueltAvvist(
            vedtaksperiodeId: UUID,
            spleisBehandlingId: UUID?,
            fødselsnummer: String,
            periodetype: Periodetype?,
            saksbehandler: Saksbehandlerløsning.Saksbehandler,
            løsning: JsonNode,
        ): VedtaksperiodeAvvist {
            return VedtaksperiodeAvvist(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                periodetype = periodetype,
                saksbehandler = saksbehandler,
                automatiskBehandlet = false,
                løsning = løsning,
            )
        }

        fun automatiskAvvist(
            vedtaksperiodeId: UUID,
            spleisBehandlingId: UUID?,
            fødselsnummer: String,
            periodetype: Periodetype?,
            saksbehandler: Saksbehandlerløsning.Saksbehandler,
            løsning: JsonNode,
        ): VedtaksperiodeAvvist {
            return VedtaksperiodeAvvist(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                periodetype = periodetype,
                saksbehandler = saksbehandler,
                automatiskBehandlet = true,
                løsning = løsning,
            )
        }
    }
}
