package no.nav.helse.modell

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDateTime

internal class UtbetalingsgodkjenningMessage(private val json: String) {
    private val behov = JsonMessage(json, MessageProblems(json))

    internal fun godkjennAutomatisk() {
        løsAutomatisk(true)
    }

    internal fun avvisAutomatisk() {
        løsAutomatisk(false)
    }

    private fun løsAutomatisk(godkjent: Boolean) {
        løs(
            automatisk = true,
            godkjent = godkjent,
            saksbehandlerIdent = "Automatisk behandlet",
            saksbehandlerEpost = "tbd@nav.no",
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = null,
            begrunnelser = null,
            kommentar = null
        )
    }

    internal fun løs(
        godkjent: Boolean,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?
    ) {
        løs(
            false,
            godkjent,
            saksbehandlerIdent,
            saksbehandlerEpost,
            godkjenttidspunkt,
            årsak,
            begrunnelser,
            kommentar
        )
    }

    private fun løs(
        automatisk: Boolean,
        godkjent: Boolean,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?
    ) {
        behov["@løsning"] = mapOf(
            "Godkjenning" to mapOf(
                "godkjent" to godkjent,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "automatiskBehandling" to automatisk,
                "årsak" to årsak,
                "begrunnelser" to begrunnelser,
                "kommentar" to kommentar
            )
        )
    }

    internal fun toJson() = behov.toJson()
    internal fun løsning() = behov["@løsning"].takeUnless { it.isMissingOrNull() }
        ?: throw RuntimeException("Forsøkte å hente ut løsning før den er satt")
}
