package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeAvvistManuelt
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentManuelt
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class GodkjenningsbehovData(
    val id: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val vedtaksperiodeId: UUID,
    val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    val utbetalingId: UUID,
    val spleisBehandlingId: UUID,
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
    val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
    private val json: String,
) {
    private lateinit var løsning: Løsning
    val erInngangsvilkårVurdertISpleis: Boolean = sykepengegrunnlagsfakta is Sykepengegrunnlagsfakta.Spleis

    fun medLøsning() =
        Godkjenningsbehovløsning(
            godkjent = løsning.godkjent,
            saksbehandlerIdent = løsning.saksbehandlerIdent,
            saksbehandlerEpost = løsning.saksbehandlerEpost,
            godkjenttidspunkt = løsning.godkjenttidspunkt,
            automatiskBehandling = løsning.automatiskBehandling,
            årsak = løsning.årsak,
            begrunnelser = løsning.begrunnelser,
            kommentar = løsning.kommentar,
            saksbehandleroverstyringer = løsning.saksbehandleroverstyringer,
            refusjonstype = løsning.refusjonstype.name,
            json = json,
        )

    private data class Løsning(
        val godkjent: Boolean,
        val saksbehandlerIdent: String,
        val saksbehandlerEpost: String,
        val godkjenttidspunkt: LocalDateTime,
        val automatiskBehandling: Boolean,
        val årsak: String?,
        val begrunnelser: List<String>?,
        val kommentar: String?,
        val saksbehandleroverstyringer: List<UUID>,
        val refusjonstype: Refusjonstype,
    )

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
            Løsning(
                godkjent = godkjent,
                saksbehandlerIdent = saksbehandlerIdent,
                saksbehandlerEpost = saksbehandlerEpost,
                godkjenttidspunkt = godkjenttidspunkt,
                automatiskBehandling = automatisk,
                årsak = årsak,
                begrunnelser = begrunnelser,
                kommentar = kommentar,
                saksbehandleroverstyringer = saksbehandleroverstyringer,
                refusjonstype = utbetaling.refusjonstype(),
            )
    }

    internal fun lagVedtaksperiodeGodkjentManuelt(
        saksbehandler: Saksbehandlerløsning.Saksbehandler,
        beslutter: Saksbehandlerløsning.Saksbehandler?,
    ) = VedtaksperiodeGodkjentManuelt(
        vedtaksperiodeId = this.vedtaksperiodeId,
        behandlingId = this.spleisBehandlingId,
        fødselsnummer = this.fødselsnummer,
        yrkesaktivitetstype = this.yrkesaktivitetstype,
        periodetype = periodetype.name,
        saksbehandlerIdent = saksbehandler.ident,
        saksbehandlerEpost = saksbehandler.epostadresse,
        beslutterIdent = beslutter?.ident,
        beslutterEpost = beslutter?.epostadresse,
    )

    internal fun lagVedtaksperiodeGodkjentAutomatisk() =
        VedtaksperiodeGodkjentAutomatisk(
            vedtaksperiodeId = this.vedtaksperiodeId,
            behandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            yrkesaktivitetstype = this.yrkesaktivitetstype,
            periodetype = periodetype.name,
        )

    internal fun lagVedtaksperiodeAvvistManuelt(saksbehandler: Saksbehandlerløsning.Saksbehandler) =
        VedtaksperiodeAvvistManuelt(
            vedtaksperiodeId = this.vedtaksperiodeId,
            behandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            yrkesaktivitetstype = this.yrkesaktivitetstype,
            periodetype = periodetype.name,
            saksbehandlerIdent = saksbehandler.ident,
            saksbehandlerEpost = saksbehandler.epostadresse,
            årsak = løsning.årsak,
            begrunnelser = løsning.begrunnelser,
            kommentar = løsning.kommentar,
        )

    internal fun lagVedtaksperiodeAvvistAutomatisk() =
        VedtaksperiodeAvvistAutomatisk(
            vedtaksperiodeId = this.vedtaksperiodeId,
            behandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            yrkesaktivitetstype = this.yrkesaktivitetstype,
            periodetype = periodetype.name,
            årsak = this.løsning.årsak,
            begrunnelser = this.løsning.begrunnelser,
            kommentar = this.løsning.kommentar,
        )
}
