package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeAvvist
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeGodkjent
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsgodkjenningMessage(json: String, private val utbetaling: Utbetaling?) {
    private val behov = JsonMessage(json, MessageProblems(json))
    private lateinit var løsning: Map<String, Any>

    private companion object {
        private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
        private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"
    }

    internal fun godkjennAutomatisk() {
        løsAutomatisk(true)
    }

    internal fun avvisAutomatisk(begrunnelser: List<String>?) {
        løsAutomatisk(false, "Automatisk avvist", begrunnelser)
    }

    private fun løsAutomatisk(
        godkjent: Boolean,
        årsak: String? = null,
        begrunnelser: List<String>? = null,
    ) {
        løs(
            behandlingId = null,
            automatisk = true,
            godkjent = godkjent,
            saksbehandlerIdent = AUTOMATISK_BEHANDLET_IDENT,
            saksbehandlerEpost = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
        )
    }

    internal fun godkjennManuelt(
        behandlingId: UUID,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandleroverstyringer: List<UUID>,
    ) {
        løsManuelt(
            behandlingId = behandlingId,
            godkjent = true,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )
    }

    internal fun avvisManuelt(
        behandlingId: UUID,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
    ) {
        løsManuelt(
            behandlingId = behandlingId,
            godkjent = false,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )
    }

    private fun løsManuelt(
        behandlingId: UUID,
        godkjent: Boolean,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
    ) {
        løs(
            behandlingId = behandlingId,
            automatisk = false,
            godkjent = godkjent,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )
    }

    private fun løs(
        behandlingId: UUID?,
        automatisk: Boolean,
        godkjent: Boolean,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
    ) {
        løsning =
            mapOf(
                "Godkjenning" to
                    mutableMapOf(
                        "godkjent" to godkjent,
                        "saksbehandlerIdent" to saksbehandlerIdent,
                        "saksbehandlerEpost" to saksbehandlerEpost,
                        "godkjenttidspunkt" to godkjenttidspunkt,
                        "automatiskBehandling" to automatisk,
                        "årsak" to årsak,
                        "begrunnelser" to begrunnelser,
                        "kommentar" to kommentar,
                        "saksbehandleroverstyringer" to saksbehandleroverstyringer,
                    ).apply {
                        compute("refusjontype") { _, _ -> utbetaling?.refusjonstype()?.name }
                    }.toMap(),
            )
        // <midlertidig forklaring="@behovId brukes for å gruppere behov/løsning. Ble innført 28. mars 2022. Må likevel fikse godkjenningsbehov som ble opprettet før 28. mars">
        behov.interestedIn("@behovId", "@id")
        if (behov["@behovId"].asText().isBlank()) behov["@behovId"] = behov["@id"].asText() // migrerer gamle behov på nytt format
        // </midlertidig>
        behov["@løsning"] = løsning
        behov["@id"] = UUID.randomUUID()
        behov["@opprettet"] = LocalDateTime.now()
        // Foreløpig opprettes behandlingId kun ved godkjenning/avvisning av oppgave. For at den ikke skal være optional utad
        // genererer vi en random uuid her. På sikt vil behandlingId sannsynligvis følge med fra vi mottar et godkjenningsbehov.
        behov["behandlingId"] = behandlingId ?: UUID.randomUUID()
    }

    internal fun lagVedtaksperiodeGodkjentManuelt(
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID?,
        fødselsnummer: String,
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        beslutter: Saksbehandlerløsning.Saksbehandler?,
        vedtakDao: VedtakDao,
    ) = VedtaksperiodeGodkjent.manueltBehandlet(
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fødselsnummer = fødselsnummer,
        periodetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

    internal fun lagVedtaksperiodeGodkjentAutomatisk(
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID?,
        fødselsnummer: String,
        vedtakDao: VedtakDao,
    ) = VedtaksperiodeGodkjent.automatiskBehandlet(
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fødselsnummer = fødselsnummer,
        periodetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId),
        saksbehandler =
            Saksbehandlerløsning.Saksbehandler(
                ident = AUTOMATISK_BEHANDLET_IDENT,
                epostadresse = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            ),
    )

    internal fun lagVedtaksperiodeAvvistManuelt(
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID?,
        fødselsnummer: String,
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        vedtakDao: VedtakDao,
    ) = VedtaksperiodeAvvist.manueltAvvist(
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fødselsnummer = fødselsnummer,
        periodetype = vedtakDao.finnVedtakId(vedtaksperiodeId)?.let { vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId) },
        saksbehandler = saksbehandler,
        løsning = objectMapper.convertValue(løsning),
    )

    internal fun lagVedtaksperiodeAvvistAutomatisk(
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID?,
        fødselsnummer: String,
        vedtakDao: VedtakDao,
    ) = VedtaksperiodeAvvist.automatiskAvvist(
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fødselsnummer = fødselsnummer,
        periodetype = vedtakDao.finnVedtakId(vedtaksperiodeId)?.let { vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId) },
        saksbehandler =
            Saksbehandlerløsning.Saksbehandler(
                ident = AUTOMATISK_BEHANDLET_IDENT,
                epostadresse = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            ),
        løsning = objectMapper.convertValue(løsning),
    )

    internal fun toJson() = behov.toJson()
}
