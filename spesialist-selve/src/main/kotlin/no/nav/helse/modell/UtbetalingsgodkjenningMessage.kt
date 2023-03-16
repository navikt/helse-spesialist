package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.convertValue
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeAvvist
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeGodkjent
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems

internal class UtbetalingsgodkjenningMessage(json: String, private val utbetaling: Utbetaling) {
    private val behov = JsonMessage(json, MessageProblems(json))
    private lateinit var løsning: Map<String, Any>

    internal fun godkjennAutomatisk() {
        løsAutomatisk(true)
    }

    internal fun avvisAutomatisk(begrunnelser: List<String>?) {
        løsAutomatisk(false, "Automatisk avvist", begrunnelser)
    }

    private fun løsAutomatisk(godkjent: Boolean, årsak: String? = null, begrunnelser: List<String>? = null) {
        løs(
            automatisk = true,
            godkjent = godkjent,
            saksbehandlerIdent = "Automatisk behandlet",
            saksbehandlerEpost = "tbd@nav.no",
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = null
        )
    }

    internal fun godkjennManuelt(
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
    ) {
        løsManuelt(true, saksbehandlerIdent, saksbehandlerEpost, godkjenttidspunkt, null, null, null)
    }

    internal fun avvisManuelt(
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?
    ) {
        løsManuelt(false, saksbehandlerIdent, saksbehandlerEpost, godkjenttidspunkt, årsak, begrunnelser, kommentar)
    }

    private fun løsManuelt(
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
        løsning = mapOf(
            "Godkjenning" to mapOf(
                "godkjent" to godkjent,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerEpost" to saksbehandlerEpost,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "automatiskBehandling" to automatisk,
                "årsak" to årsak,
                "begrunnelser" to begrunnelser,
                "kommentar" to kommentar,
                "refusjonstype" to utbetaling.refusjonstype().name
            )
        )
        // <midlertidig forklaring="@behovId brukes for å gruppere behov/løsning. Ble innført 28. mars 2022. Må likevel fikse godkjenningsbehov som ble opprettet før 28. mars">
        behov.interestedIn("@behovId", "@id")
        if (behov["@behovId"].asText().isBlank()) behov["@behovId"] = behov["@id"].asText() // migrerer gamle behov på nytt format
        // </midlertidig>
        behov["@løsning"] = løsning
        behov["@id"] = UUID.randomUUID()
        behov["@opprettet"] = LocalDateTime.now()
    }

    internal fun lagVedtaksperiodeGodkjent(
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        warningDao: WarningDao,
        vedtakDao: VedtakDao
    ) =
        VedtaksperiodeGodkjent(
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId).map { it.dto() },
            periodetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId),
            løsning = objectMapper.convertValue(løsning)
        )

    internal fun lagVedtaksperiodeAvvist(
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        warningDao: WarningDao,
        vedtakDao: VedtakDao
    ) = VedtaksperiodeAvvist(
        vedtaksperiodeId = vedtaksperiodeId,
        fødselsnummer = fødselsnummer,
        warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId).map { it.dto() },
        periodetype = vedtakDao.finnVedtakId(vedtaksperiodeId)?.let { vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId) },
        løsning = objectMapper.convertValue(løsning)
    )

    internal fun toJson() = behov.toJson()
}
