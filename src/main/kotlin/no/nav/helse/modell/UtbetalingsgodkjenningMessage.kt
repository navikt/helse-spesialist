package no.nav.helse.modell

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDateTime

internal class UtbetalingsgodkjenningMessage(json: String) {
    private val behov = JsonMessage(json, MessageProblems(json))

    internal fun godkjennAutomatisk() {
        løsAutomatisk(true)
    }

    internal fun avvisAutomatisk(begrunnelser: List<String>?) {
        løsAutomatisk(false, "Automatisk avvist", begrunnelser)
    }

    internal fun makstidOppnådd(begrunnelser: List<String>?) {
        løsAutomatisk(false, "Automatisk avvist", begrunnelser, true)
    }

    private fun løsAutomatisk(godkjent: Boolean, årsak: String? = null, begrunnelser: List<String>? = null, makstidOppnådd: Boolean = false) {
        løs(
            automatisk = true,
            godkjent = godkjent,
            saksbehandlerIdent = "Automatisk behandlet",
            saksbehandlerEpost = "tbd@nav.no",
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = null,
            makstidOppnådd = makstidOppnådd
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
            kommentar,
            false
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
        kommentar: String?,
        makstidOppnådd: Boolean
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
                "kommentar" to kommentar,
                "makstidOppnådd" to makstidOppnådd
            )
        )
    }

    internal fun toJson() = behov.toJson()
    internal fun løsning() = behov["@løsning"].takeUnless { it.isMissingOrNull() }
        ?: throw RuntimeException("Forsøkte å hente ut løsning før den er satt")
}
