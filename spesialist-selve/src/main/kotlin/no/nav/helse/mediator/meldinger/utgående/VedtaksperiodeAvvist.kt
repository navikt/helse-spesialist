package no.nav.helse.mediator.meldinger.utgående

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.rapids_rivers.JsonMessage

internal class VedtaksperiodeAvvist private constructor(
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID?,
    val fødselsnummer: String,
    val periodetype: Periodetype?,
    val saksbehandler: Saksbehandlerløsning.Saksbehandler,
    val automatiskBehandlet: Boolean,
    val løsning: JsonNode
) {
    internal fun toJson() =
        JsonMessage.newMessage("vedtaksperiode_avvist", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "saksbehandlerIdent" to saksbehandler.ident,
            "saksbehandlerEpost" to saksbehandler.epostadresse,
            "saksbehandler" to mapOf(
                "ident" to saksbehandler.ident,
                "epostadresse" to saksbehandler.epostadresse
            ),
            "automatiskBehandling" to automatiskBehandlet,
            "årsak" to løsning["Godkjenning"]["årsak"].asText(),
            "begrunnelser" to løsning["Godkjenning"]["begrunnelser"].map(JsonNode::asText),
            "kommentar" to løsning["Godkjenning"]["kommentar"].asText()
            ).apply {
                compute("periodetype") { _, _ -> periodetype?.name }
                compute("behandlingId") { _, _ -> spleisBehandlingId }
            }
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
                løsning = løsning
            )
        }

        fun automatiskAvvist(
            vedtaksperiodeId: UUID,
            spleisBehandlingId: UUID?,
            fødselsnummer: String,
            periodetype: Periodetype?,
            saksbehandler: Saksbehandlerløsning.Saksbehandler,
            løsning: JsonNode
        ): VedtaksperiodeAvvist {
            return VedtaksperiodeAvvist(
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fødselsnummer = fødselsnummer,
                periodetype = periodetype,
                saksbehandler = saksbehandler,
                automatiskBehandlet = true,
                løsning =  løsning
            )
        }
    }
}
