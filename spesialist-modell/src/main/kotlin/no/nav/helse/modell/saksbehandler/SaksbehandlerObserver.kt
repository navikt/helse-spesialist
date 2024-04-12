package no.nav.helse.modell.saksbehandler

import no.nav.helse.modell.vilkårsprøving.SubsumsjonEvent
import java.time.LocalDate
import java.util.UUID

interface SaksbehandlerObserver {
    fun tidslinjeOverstyrt(
        fødselsnummer: String,
        event: OverstyrtTidslinjeEvent,
    ) {}

    fun inntektOgRefusjonOverstyrt(
        fødselsnummer: String,
        event: OverstyrtInntektOgRefusjonEvent,
    ) {}

    fun arbeidsforholdOverstyrt(
        fødselsnummer: String,
        event: OverstyrtArbeidsforholdEvent,
    ) {}

    fun sykepengegrunnlagSkjønnsfastsatt(
        fødselsnummer: String,
        event: SkjønnsfastsattSykepengegrunnlagEvent,
    ) {}

    fun utbetalingAnnullert(
        fødselsnummer: String,
        event: AnnullertUtbetalingEvent,
    ) {}

    fun nySubsumsjon(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
    ) {}
}

data class OverstyrtInntektOgRefusjonEvent(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiverEvent>,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
) {
    data class OverstyrtArbeidsgiverEvent(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<OverstyrtRefusjonselementEvent>?,
        val fraRefusjonsopplysninger: List<OverstyrtRefusjonselementEvent>?,
        val begrunnelse: String,
        val forklaring: String,
    ) {
        data class OverstyrtRefusjonselementEvent(
            val fom: LocalDate,
            val tom: LocalDate? = null,
            val beløp: Double,
        )
    }
}

data class OverstyrtTidslinjeEvent(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjeEventDag>,
) {
    data class OverstyrtTidslinjeEventDag(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

data class OverstyrtArbeidsforholdEvent(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
) {
    data class Arbeidsforhold(
        val orgnummer: String,
        val deaktivert: Boolean,
        val begrunnelse: String,
        val forklaring: String,
    )
}

data class SkjønnsfastsattSykepengegrunnlagEvent(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
) {
    data class SkjønnsfastsattArbeidsgiver(
        val organisasjonsnummer: String,
        val årlig: Double,
        val fraÅrlig: Double,
        val årsak: String,
        val type: String,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val initierendeVedtaksperiodeId: String?,
    )
}

data class AnnullertUtbetalingEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val begrunnelser: List<String>,
    val kommentar: String?,
)
