package no.nav.helse.kafka.message_builders

import no.nav.helse.modell.hendelse.UtgåendeHendelse
import no.nav.helse.rapids_rivers.JsonMessage

private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"

internal fun UtgåendeHendelse.somJsonMessage(fødselsnummer: String): JsonMessage {
    return JsonMessage.newMessage(eventName(), mapOf("fødselsnummer" to fødselsnummer) + detaljer())
}

internal fun UtgåendeHendelse.eventName() =
    when (this) {
        is UtgåendeHendelse.VedtaksperiodeAvvistAutomatisk,
        is UtgåendeHendelse.VedtaksperiodeAvvistManuelt,
        -> "vedtaksperiode_avvist"
        is UtgåendeHendelse.VedtaksperiodeGodkjentAutomatisk,
        is UtgåendeHendelse.VedtaksperiodeGodkjentManuelt,
        -> "vedtaksperiode_godkjent"
    }

private fun UtgåendeHendelse.detaljer(): Map<String, Any> {
    return when (this) {
        is UtgåendeHendelse.VedtaksperiodeAvvistManuelt -> this.detaljer()
        is UtgåendeHendelse.VedtaksperiodeAvvistAutomatisk -> this.detaljer()
        is UtgåendeHendelse.VedtaksperiodeGodkjentManuelt -> this.detaljer()
        is UtgåendeHendelse.VedtaksperiodeGodkjentAutomatisk -> this.detaljer()
    }
}

private fun UtgåendeHendelse.VedtaksperiodeAvvistAutomatisk.detaljer(): Map<String, Any> {
    return buildMap {
        put("fødselsnummer", fødselsnummer)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("saksbehandlerIdent", AUTOMATISK_BEHANDLET_IDENT)
        put("saksbehandlerEpost", AUTOMATISK_BEHANDLET_EPOSTADRESSE)
        put(
            "saksbehandler",
            mapOf(
                "ident" to AUTOMATISK_BEHANDLET_IDENT,
                "epostadresse" to AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            ),
        )
        put("automatiskBehandling", true)
        compute("årsak") { _, _ -> årsak }
        compute("begrunnelser") { _, _ -> begrunnelser }
        compute("kommentar") { _, _ -> kommentar }
        put("periodetype", periodetype)
        put("behandlingId", behandlingId)
    }
}

private fun UtgåendeHendelse.VedtaksperiodeAvvistManuelt.detaljer(): Map<String, Any> {
    return buildMap {
        put("fødselsnummer", fødselsnummer)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("saksbehandlerIdent", saksbehandlerIdent)
        put("saksbehandlerEpost", saksbehandlerEpost)
        put(
            "saksbehandler",
            mapOf(
                "ident" to saksbehandlerIdent,
                "epostadresse" to saksbehandlerEpost,
            ),
        )
        put("automatiskBehandling", false)
        compute("årsak") { _, _ -> årsak }
        compute("begrunnelser") { _, _ -> begrunnelser }
        compute("kommentar") { _, _ -> kommentar }
        put("periodetype", periodetype)
        put("behandlingId", behandlingId)
    }
}

private fun UtgåendeHendelse.VedtaksperiodeGodkjentAutomatisk.detaljer(): Map<String, Any> {
    return mapOf(
        "fødselsnummer" to fødselsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "periodetype" to periodetype,
        "saksbehandlerIdent" to AUTOMATISK_BEHANDLET_IDENT,
        "saksbehandlerEpost" to AUTOMATISK_BEHANDLET_EPOSTADRESSE,
        "automatiskBehandling" to true,
        "saksbehandler" to
            mapOf(
                "ident" to AUTOMATISK_BEHANDLET_IDENT,
                "epostadresse" to AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            ),
        "behandlingId" to behandlingId,
    )
}

private fun UtgåendeHendelse.VedtaksperiodeGodkjentManuelt.detaljer(): Map<String, Any> {
    return buildMap {
        put("fødselsnummer", fødselsnummer)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("periodetype", periodetype)
        put("saksbehandlerIdent", saksbehandlerIdent)
        put("saksbehandlerEpost", saksbehandlerEpost)
        put("automatiskBehandling", false)
        put(
            "saksbehandler",
            mapOf(
                "ident" to saksbehandlerIdent,
                "epostadresse" to saksbehandlerEpost,
            ),
        )
        if (beslutterEpost != null && beslutterIdent != null) {
            put(
                "beslutter",
                mapOf(
                    "ident" to beslutterIdent,
                    "epostadresse" to beslutterEpost,
                ),
            )
        }
        put("behandlingId", behandlingId)
    }
}
