package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.utbetaling.Utbetalingtype
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
    val sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta,
    val foreløpigBeregnetSluttPåSykepenger: LocalDate,
    val relevanteSøknader: List<UUID>,
    private val json: String,
) {
    private lateinit var løsning: Løsning

    fun medLøsning() =
        when (val løsning = løsning) {
            is Løsning.Automatisk -> {
                Godkjenningsbehovløsning(
                    godkjent = løsning.godkjent,
                    saksbehandlerIdent = AUTOMATISK_BEHANDLET_IDENT,
                    saksbehandlerEpost = AUTOMATISK_BEHANDLET_EPOSTADRESSE,
                    godkjenttidspunkt = løsning.godkjenttidspunkt,
                    automatiskBehandling = true,
                    årsak = løsning.årsak,
                    begrunnelser = løsning.begrunnelser,
                    kommentar = null,
                    saksbehandleroverstyringer = emptyList(),
                    json = json,
                )
            }

            is Løsning.Manuelt -> {
                Godkjenningsbehovløsning(
                    godkjent = løsning.godkjent,
                    saksbehandlerIdent = løsning.saksbehandlerIdent,
                    saksbehandlerEpost = løsning.saksbehandlerEpost,
                    godkjenttidspunkt = løsning.godkjenttidspunkt,
                    automatiskBehandling = false,
                    årsak = løsning.årsak,
                    begrunnelser = løsning.begrunnelser,
                    kommentar = løsning.kommentar,
                    saksbehandleroverstyringer = løsning.saksbehandleroverstyringer,
                    json = json,
                )
            }
        }

    private sealed class Løsning(
        val godkjent: Boolean,
        val godkjenttidspunkt: LocalDateTime,
        val årsak: String?,
        val begrunnelser: List<String>?,
    ) {
        class Automatisk(
            godkjent: Boolean,
            godkjenttidspunkt: LocalDateTime,
            årsak: String?,
            begrunnelser: List<String>?,
        ) : Løsning(
                godkjent,
                godkjenttidspunkt,
                årsak,
                begrunnelser,
            )

        class Manuelt(
            godkjent: Boolean,
            godkjenttidspunkt: LocalDateTime,
            årsak: String?,
            begrunnelser: List<String>?,
            val saksbehandlerIdent: String,
            val saksbehandlerEpost: String,
            val kommentar: String?,
            val saksbehandleroverstyringer: List<UUID>,
        ) : Løsning(godkjent, godkjenttidspunkt, årsak, begrunnelser)
    }

    private companion object {
        private const val AUTOMATISK_BEHANDLET_IDENT = "Automatisk behandlet"
        private const val AUTOMATISK_BEHANDLET_EPOSTADRESSE = "tbd@nav.no"
    }

    internal fun godkjennAutomatisk() {
        løsAutomatisk(true)
    }

    internal fun avvisAutomatisk(
        begrunnelser: List<String>?,
    ) {
        løsAutomatisk(false, "Automatisk avvist", begrunnelser)
    }

    fun godkjennManuelt(
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandleroverstyringer: List<UUID>,
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
        )
    }

    fun avvisManuelt(
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        avvisttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
    ) {
        løsManuelt(
            godkjent = false,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = avvisttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
        )
    }

    private fun løsAutomatisk(
        godkjent: Boolean,
        årsak: String? = null,
        begrunnelser: List<String>? = null,
    ) {
        løsning =
            Løsning.Automatisk(
                godkjent = godkjent,
                godkjenttidspunkt = LocalDateTime.now(),
                årsak = årsak,
                begrunnelser = begrunnelser,
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
    ) {
        løsning =
            Løsning.Manuelt(
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

    internal fun lagVedtaksperiodeGodkjentAutomatisk() =
        VedtaksperiodeGodkjentAutomatisk(
            vedtaksperiodeId = this.vedtaksperiodeId,
            behandlingId = this.spleisBehandlingId,
            fødselsnummer = this.fødselsnummer,
            yrkesaktivitetstype = this.yrkesaktivitetstype,
            periodetype = periodetype.name,
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
        )
}
