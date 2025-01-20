package no.nav.helse.kafka.message_builders

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.HentDokument
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.modell.melding.OppdaterPersondata
import no.nav.helse.modell.melding.Saksbehandlerløsning
import no.nav.helse.modell.melding.Sykepengevedtak
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.objectMapper
import java.time.LocalDateTime
import java.util.UUID

private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"

fun UtgåendeHendelse.somJsonMessage(fødselsnummer: String): JsonMessage {
    return JsonMessage.newMessage(eventName(), mapOf("fødselsnummer" to fødselsnummer) + detaljer())
}

internal fun UtgåendeHendelse.eventName() =
    when (this) {
        is VedtaksperiodeAvvistAutomatisk,
        is VedtaksperiodeAvvistManuelt,
        -> "vedtaksperiode_avvist"

        is VedtaksperiodeGodkjentAutomatisk,
        is VedtaksperiodeGodkjentManuelt,
        -> "vedtaksperiode_godkjent"

        is Godkjenningsbehovløsning -> "behov"
        is Sykepengevedtak -> "vedtak_fattet"
        is KlargjørPersonForVisning -> "klargjør_person_for_visning"
        is OppdaterPersondata -> "oppdater_persondata"
        is Saksbehandlerløsning -> "saksbehandler_løsning"
        is HentDokument -> "hent-dokument"
    }

private fun UtgåendeHendelse.detaljer(): Map<String, Any> {
    return when (this) {
        is VedtaksperiodeAvvistManuelt -> this.detaljer()
        is VedtaksperiodeAvvistAutomatisk -> this.detaljer()
        is VedtaksperiodeGodkjentManuelt -> this.detaljer()
        is VedtaksperiodeGodkjentAutomatisk -> this.detaljer()
        is Godkjenningsbehovløsning -> this.detaljer()
        is Sykepengevedtak -> this.detaljer()
        is KlargjørPersonForVisning -> emptyMap()
        is OppdaterPersondata -> emptyMap()
        is Saksbehandlerløsning -> this.detaljer()
        is HentDokument -> this.detaljer()
    }
}

private fun Godkjenningsbehovløsning.detaljer(): Map<String, Any> {
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
                            årsak?.let { put("årsak", it) }
                            begrunnelser?.let { put("begrunnelser", it) }
                            kommentar?.let { put("kommentar", it) }
                            put("saksbehandleroverstyringer", saksbehandleroverstyringer)
                            put("refusjontype", refusjonstype)
                        },
                ),
        )
    return orginaltBehov +
        løsning +
        mapOf("@id" to UUID.randomUUID(), "@opprettet" to LocalDateTime.now())
}

private fun VedtaksperiodeAvvistAutomatisk.detaljer(): Map<String, Any> {
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
        årsak?.let { put("årsak", it) }
        begrunnelser?.let { put("begrunnelser", it) }
        kommentar?.let { put("kommentar", it) }
        put("periodetype", periodetype)
        put("behandlingId", behandlingId)
    }
}

private fun VedtaksperiodeAvvistManuelt.detaljer(): Map<String, Any> {
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
        årsak?.let { put("årsak", it) }
        begrunnelser?.let { put("begrunnelser", it) }
        kommentar?.let { put("kommentar", it) }
        put("periodetype", periodetype)
        put("behandlingId", behandlingId)
    }
}

private fun VedtaksperiodeGodkjentAutomatisk.detaljer(): Map<String, Any> {
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

private fun VedtaksperiodeGodkjentManuelt.detaljer(): Map<String, Any> {
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

private fun Saksbehandlerløsning.detaljer(): Map<String, Any> =
    listOfNotNull(
        "@forårsaket_av" to
            mapOf(
                "event_name" to "behov",
                "behov" to "Godkjenning",
                "id" to godkjenningsbehovId,
            ),
        "oppgaveId" to oppgaveId,
        "hendelseId" to godkjenningsbehovId,
        "godkjent" to godkjent,
        "saksbehandlerident" to saksbehandlerIdent,
        "saksbehandleroid" to saksbehandlerOid,
        "saksbehandlerepost" to saksbehandlerEpost,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "saksbehandleroverstyringer" to saksbehandleroverstyringer,
        "saksbehandler" to
            mapOf(
                "ident" to saksbehandler.ident(),
                "epostadresse" to saksbehandler.epostadresse(),
            ),
        årsak?.let { "årsak" to it },
        begrunnelser?.let { "begrunnelser" to it },
        kommentar?.let { "kommentar" to it },
        beslutter?.let {
            "beslutter" to
                mapOf(
                    "ident" to it.ident(),
                    "epostadresse" to it.epostadresse(),
                )
        },
    ).toMap()

private fun HentDokument.detaljer(): Map<String, Any> =
    mapOf(
        "dokumentId" to dokumentId,
        "dokumentType" to dokumentType,
    )
