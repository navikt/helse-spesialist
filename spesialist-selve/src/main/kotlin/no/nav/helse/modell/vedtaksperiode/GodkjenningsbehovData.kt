package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeAvvist
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeGodkjent
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class GodkjenningsbehovData(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    val utbetalingId: UUID,
    val spleisBehandlingId: UUID,
    val avviksvurderingId: UUID?,
    val vilkårsgrunnlagId: UUID,
    val tags: List<String>,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val periodetype: Periodetype,
    val førstegangsbehandling: Boolean,
    val utbetalingtype: Utbetalingtype,
    val kanAvvises: Boolean,
    val inntektskilde: Inntektskilde,
    val orgnummereMedRelevanteArbeidsforhold: List<String>,
    val skjæringstidspunkt: LocalDate,
    private val json: String,
) {
    private val behov = JsonMessage(json, MessageProblems(json))
    private lateinit var løsning: Map<String, Any>

    private companion object {
        private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
        private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"
    }

    internal fun godkjennAutomatisk(utbetaling: Utbetaling) {
        løsAutomatisk(true, utbetaling)
    }

    internal fun avvisAutomatisk(
        utbetaling: Utbetaling,
        begrunnelser: List<String>?,
    ) {
        løsAutomatisk(false, utbetaling, "Automatisk avvist", begrunnelser)
    }

    internal fun godkjennManuelt(
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandleroverstyringer: List<UUID>,
        utbetaling: Utbetaling,
    ) {
        løsManuelt(
            godkjent = true,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            utbetaling = utbetaling,
        )
    }

    internal fun avvisManuelt(
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
        utbetaling: Utbetaling,
    ) {
        løsManuelt(
            godkjent = false,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            utbetaling = utbetaling,
        )
    }

    private fun løsAutomatisk(
        godkjent: Boolean,
        utbetaling: Utbetaling,
        årsak: String? = null,
        begrunnelser: List<String>? = null,
    ) {
        løs(
            automatisk = true,
            godkjent = godkjent,
            saksbehandlerIdent = AUTOMATISK_BEHANDLET_IDENT,
            saksbehandlerEpost = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            utbetaling = utbetaling,
        )
    }

    private fun løsManuelt(
        godkjent: Boolean,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
        utbetaling: Utbetaling,
    ) {
        løs(
            automatisk = false,
            godkjent = godkjent,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            utbetaling = utbetaling,
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
        saksbehandleroverstyringer: List<UUID>,
        utbetaling: Utbetaling,
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
                        compute("refusjontype") { _, _ -> utbetaling.refusjonstype().name }
                    }.toMap(),
            )
        behov["@løsning"] = løsning
        behov["@id"] = UUID.randomUUID()
        behov["@opprettet"] = LocalDateTime.now()
    }

    internal fun lagVedtaksperiodeGodkjentManuelt(
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        beslutter: Saksbehandlerløsning.Saksbehandler?,
    ) = VedtaksperiodeGodkjent.manueltBehandlet(
        vedtaksperiodeId = this.vedtaksperiodeId,
        spleisBehandlingId = this.spleisBehandlingId,
        fødselsnummer = this.fødselsnummer,
        periodetype = periodetype,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

    internal fun lagVedtaksperiodeGodkjentAutomatisk() =
        VedtaksperiodeGodkjent.automatiskBehandlet(
            vedtaksperiodeId = this.vedtaksperiodeId,
            spleisBehandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            periodetype = periodetype,
            saksbehandler =
                Saksbehandlerløsning.Saksbehandler(
                    ident = AUTOMATISK_BEHANDLET_IDENT,
                    epostadresse = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
                ),
        )

    internal fun lagVedtaksperiodeAvvistManuelt(saksbehandler: Saksbehandlerløsning.Saksbehandler) =
        VedtaksperiodeAvvist.manueltAvvist(
            vedtaksperiodeId = this.vedtaksperiodeId,
            spleisBehandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            periodetype = periodetype,
            saksbehandler = saksbehandler,
            løsning = objectMapper.convertValue(løsning),
        )

    internal fun lagVedtaksperiodeAvvistAutomatisk() =
        VedtaksperiodeAvvist.automatiskAvvist(
            vedtaksperiodeId = this.vedtaksperiodeId,
            spleisBehandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            periodetype = periodetype,
            saksbehandler =
                Saksbehandlerløsning.Saksbehandler(
                    ident = AUTOMATISK_BEHANDLET_IDENT,
                    epostadresse = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
                ),
            løsning = objectMapper.convertValue(løsning),
        )

    internal fun toJson() = behov.toJson()
}
