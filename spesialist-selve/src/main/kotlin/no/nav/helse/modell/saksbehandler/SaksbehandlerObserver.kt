package no.nav.helse.modell.saksbehandler

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.handlinger.SubsumsjonEvent
import no.nav.helse.rapids_rivers.JsonMessage

interface SaksbehandlerObserver {
    fun tidslinjeOverstyrt(fødselsnummer: String, event: OverstyrtTidslinjeEvent) {}
    fun inntektOgRefusjonOverstyrt(fødselsnummer: String, event: OverstyrtInntektOgRefusjonEvent) {}
    fun arbeidsforholdOverstyrt(fødselsnummer: String, event: OverstyrtArbeidsforholdEvent) {}
    fun sykepengegrunnlagSkjønnsfastsatt(fødselsnummer: String, event: SkjønnsfastsattSykepengegrunnlagEvent) {}
    fun utbetalingAnnullert(fødselsnummer: String, event: AnnullertUtbetalingEvent) {}
    fun nySubsumsjon(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent) {}
}

data class OverstyrtInntektOgRefusjonEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiverEvent>,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String
) {
    fun somJsonMessage() = JsonMessage.newMessage(
        "saksbehandler_overstyrer_inntekt_og_refusjon",
        listOfNotNull(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "arbeidsgivere" to arbeidsgivere,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerIdent" to saksbehandlerIdent,
            "saksbehandlerEpost" to saksbehandlerEpost,
        ).toMap()
    )

    data class OverstyrtArbeidsgiverEvent(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<OverstyrtRefusjonselementEvent>?,
        val fraRefusjonsopplysninger: List<OverstyrtRefusjonselementEvent>?,
        val begrunnelse: String,
        val forklaring: String,
        val subsumsjon: LovhjemmelEvent?,
    ) {
        data class OverstyrtRefusjonselementEvent(
            val fom: LocalDate,
            val tom: LocalDate? = null,
            val beløp: Double
        )
    }
}

data class LovhjemmelEvent(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
    val lovverk: String,
    val lovverksversjon: String,
)

data class OverstyrtTidslinjeEvent(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjeEventDag>,
) {
    //TODO: JsonMessage burde ikke være kjent for modell, bør mappes om fra Event til JsonMessage i mediator
    fun somJsonMessage(): JsonMessage {
        return JsonMessage.newMessage(
            eventName(), mutableMapOf(
                "@id" to id,
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "dager" to dager,
            )
        )
    }

    fun eventName() = "overstyr_tidslinje"

    data class OverstyrtTidslinjeEventDag(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

data class OverstyrtArbeidsforholdEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
) {
    fun somJsonMessage() = JsonMessage.newMessage(
        "saksbehandler_overstyrer_arbeidsforhold", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerIdent" to saksbehandlerIdent,
            "saksbehandlerEpost" to saksbehandlerEpost,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "overstyrteArbeidsforhold" to overstyrteArbeidsforhold,
        )
    )

    data class Arbeidsforhold(
        val orgnummer: String,
        val deaktivert: Boolean,
        val begrunnelse: String,
        val forklaring: String,
    )
}

data class SkjønnsfastsattSykepengegrunnlagEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String,
    val saksbehandlerEpost: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>
) {
    fun somJsonMessage() = JsonMessage.newMessage(
        "saksbehandler_skjonnsfastsetter_sykepengegrunnlag",
        listOfNotNull(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "arbeidsgivere" to arbeidsgivere,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerIdent" to saksbehandlerIdent,
            "saksbehandlerEpost" to saksbehandlerEpost,
        ).toMap()
    )

    data class SkjønnsfastsattArbeidsgiver(
        val organisasjonsnummer: String,
        val årlig: Double,
        val fraÅrlig: Double,
        val årsak: String,
        val type: String,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val subsumsjon: LovhjemmelEvent?,
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
    val fagsystemId: String,
    val begrunnelser: List<String>,
    val kommentar: String?
) {
    fun somJsonMessage(): JsonMessage {
        return JsonMessage.newMessage(
            "annullering", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "aktørId" to aktørId,
                "saksbehandler" to mapOf(
                    "epostaddresse" to saksbehandlerEpost,
                    "oid" to saksbehandlerOid,
                    "navn" to saksbehandlerNavn,
                    "ident" to saksbehandlerIdent,
                ),
                "fagsystemId" to fagsystemId,
                "begrunnelser" to begrunnelser,
            ).apply {
                compute("kommentar") { _, _ -> kommentar }
            }
        )
    }
}