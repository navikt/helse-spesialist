package no.nav.helse.mediator.meldinger.utgående

import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.rapids_rivers.JsonMessage

internal class VedtaksperiodeGodkjent private constructor(
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val periodetype: Periodetype,
    private val saksbehandler: Saksbehandlerløsning.Saksbehandler,
    private val beslutter: Saksbehandlerløsning.Saksbehandler?,
    private val automatiskBehandlet: Boolean
) {
    internal fun toJson() =
        JsonMessage.newMessage(
            "vedtaksperiode_godkjent",
            mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "periodetype" to periodetype.name,
                "saksbehandlerIdent" to saksbehandler.ident,
                "saksbehandlerEpost" to saksbehandler.epostadresse,
                "automatiskBehandling" to automatiskBehandlet,
                "saksbehandler" to mapOf(
                    "ident" to saksbehandler.ident,
                    "epostadresse" to saksbehandler.epostadresse
                )
            ).apply {
                compute("beslutter") { _, _ ->
                    beslutter?.let {
                        mapOf("ident" to beslutter.ident, "epostadresse" to beslutter.epostadresse)
                    }
                }
            }
        ).toJson()

    internal companion object {
        fun manueltBehandlet(
            vedtaksperiodeId: UUID,
            fødselsnummer: String,
            periodetype: Periodetype,
            saksbehandler: Saksbehandlerløsning.Saksbehandler,
            beslutter: Saksbehandlerløsning.Saksbehandler?,
        ): VedtaksperiodeGodkjent {
            return VedtaksperiodeGodkjent(
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                periodetype = periodetype,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                automatiskBehandlet = false
            )
        }

        fun automatiskBehandlet(
            vedtaksperiodeId: UUID,
            fødselsnummer: String,
            periodetype: Periodetype,
            saksbehandler: Saksbehandlerløsning.Saksbehandler
        ): VedtaksperiodeGodkjent {
            return VedtaksperiodeGodkjent(
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                periodetype = periodetype,
                saksbehandler = saksbehandler,
                beslutter = null,
                automatiskBehandlet = true
            )
        }
    }
}
