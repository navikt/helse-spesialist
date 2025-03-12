package no.nav.helse.modell.melding

import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import java.time.LocalDate
import java.util.UUID

data class OverstyrtInntektOgRefusjonEvent(
    val eksternHendelseId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiverEvent>,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
) : UtgåendeHendelse {
    data class OverstyrtArbeidsgiverEvent(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<OverstyrtRefusjonselementEvent>?,
        val fraRefusjonsopplysninger: List<OverstyrtRefusjonselementEvent>?,
        val begrunnelse: String,
        val forklaring: String,
        val fom: LocalDate?,
        val tom: LocalDate?,
    ) {
        data class OverstyrtRefusjonselementEvent(
            val fom: LocalDate,
            val tom: LocalDate? = null,
            val beløp: Double,
        )
    }
}

data class OverstyrtTidslinjeEvent(
    val eksternHendelseId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjeEventDag>,
) : UtgåendeHendelse {
    data class OverstyrtTidslinjeEventDag(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

data class OverstyrtArbeidsforholdEvent(
    val eksternHendelseId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
) : UtgåendeHendelse {
    data class Arbeidsforhold(
        val orgnummer: String,
        val deaktivert: Boolean,
        val begrunnelse: String,
        val forklaring: String,
    )
}

data class SkjønnsfastsattSykepengegrunnlagEvent(
    val eksternHendelseId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
) : UtgåendeHendelse {
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

data class MinimumSykdomsgradVurdertEvent(
    val eksternHendelseId: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val perioderMedMinimumSykdomsgradVurdertOk: List<MinimumSykdomsgradPeriode>,
    val perioderMedMinimumSykdomsgradVurdertIkkeOk: List<MinimumSykdomsgradPeriode>,
) : UtgåendeHendelse

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
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val begrunnelser: List<String>,
    val arsaker: List<AnnulleringArsak>?,
    val kommentar: String?,
) : UtgåendeHendelse

data class LagtPåVentEvent(
    val fødselsnummer: String,
    val oppgaveId: Long,
    val behandlingId: UUID,
    val skalTildeles: Boolean,
    val frist: LocalDate,
    val notatTekst: String?,
    val årsaker: List<PåVentÅrsak>,
    val saksbehandlerOid: UUID,
    val saksbehandlerIdent: String,
) : UtgåendeHendelse

data class InntektsendringerEvent(
    val inntektskildeendringer: List<Inntektskildeendring>,
) : UtgåendeHendelse {
    data class Inntektskildeendring(
        val organisasjonsnummer: String,
        val nyeEllerEndredeInntekter: List<PeriodeMedBeløp>,
        val fjernedeInntekter: List<PeriodeUtenBeløp>,
    ) {
        override fun toString(): String =
            buildString {
                append(organisasjonsnummer).append(": \n\t")
                nyeEllerEndredeInntekter
                    .takeUnless { it.isEmpty() }
                    ?.joinTo(this, separator = "\n\t") { it.toString() }
                    ?.append("\n")
                if (nyeEllerEndredeInntekter.isNotEmpty() && fjernedeInntekter.isNotEmpty()) append("\n\t")
                fjernedeInntekter
                    .takeUnless { it.isEmpty() }
                    ?.joinTo(this, separator = "\n\t") { it.toString() }
                    ?.appendLine()
            }

        data class PeriodeMedBeløp(
            val fom: LocalDate,
            val tom: LocalDate,
            val periodebeløp: Double,
        )

        data class PeriodeUtenBeløp(
            val fom: LocalDate,
            val tom: LocalDate,
        )
    }

    override fun toString() = inntektskildeendringer.joinToString(separator = "\n")
}
