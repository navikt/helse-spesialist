package no.nav.helse.kafka.message_builders

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.hendelse.UtgåendeHendelse
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.UUID

private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"

internal fun UtgåendeHendelse.somJsonMessage(fødselsnummer: String): JsonMessage {
    return when (this) {
        is UtgåendeHendelse.Godkjenningsbehovløsning -> somBehovløsning()
        else -> JsonMessage.newMessage(eventName(), mapOf("fødselsnummer" to fødselsnummer) + detaljer())
    }
}

private fun UtgåendeHendelse.Godkjenningsbehovløsning.somBehovløsning(): JsonMessage {
    val orginaltBehov = objectMapper.readValue<Map<String, Any>>(this.json)
    val løsning =
        mapOf(
            "@løsning" to
                mapOf(
                    "Godkjenning" to
                        buildMap {
                            put("godkjent", godkjent)
                            put("saksbehandlerIdent", saksbehandlerIdent)
                            put("saksbehandlerEpost", saksbehandlerEpost)
                            put("godkjenttidspunkt", godkjenttidspunkt)
                            put("automatiskBehandling", automatiskBehandling)
                            if (årsak != null) put("årsak", årsak)
                            if (begrunnelser != null) put("begrunnelser", begrunnelser)
                            if (kommentar != null) put("kommentar", kommentar)
                            put("saksbehandleroverstyringer", saksbehandleroverstyringer)
                            put("refusjontype", refusjonstype)
                        },
                ),
        )
    return JsonMessage.newMessage(
        mapOf("@id" to UUID.randomUUID(), "@opprettet" to LocalDateTime.now()) +
            orginaltBehov +
            løsning,
    )
}

internal fun UtgåendeHendelse.eventName() =
    when (this) {
        is UtgåendeHendelse.VedtaksperiodeAvvistAutomatisk,
        is UtgåendeHendelse.VedtaksperiodeAvvistManuelt,
        -> "vedtaksperiode_avvist"
        is UtgåendeHendelse.VedtaksperiodeGodkjentAutomatisk,
        is UtgåendeHendelse.VedtaksperiodeGodkjentManuelt,
        -> "vedtaksperiode_godkjent"

        is UtgåendeHendelse.Godkjenningsbehovløsning -> "behov"
    }

private fun UtgåendeHendelse.detaljer(): Map<String, Any> {
    return when (this) {
        is UtgåendeHendelse.VedtaksperiodeAvvistManuelt -> this.detaljer()
        is UtgåendeHendelse.VedtaksperiodeAvvistAutomatisk -> this.detaljer()
        is UtgåendeHendelse.VedtaksperiodeGodkjentManuelt -> this.detaljer()
        is UtgåendeHendelse.VedtaksperiodeGodkjentAutomatisk -> this.detaljer()
        is UtgåendeHendelse.Godkjenningsbehovløsning -> TODO()
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
