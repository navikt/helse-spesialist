package no.nav.helse.modell.melding

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SaksbehandlerIdentOgNavn(
    val ident: String,
    val navn: String,
)

data class VedtakFattetMelding(
    val fødselsnummer: String,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val organisasjonsnummer: String,
    val yrkesaktivitetstype: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val hendelser: List<UUID>,
    val sykepengegrunnlag: BigDecimal,
    val vedtakFattetTidspunkt: LocalDateTime,
    val utbetalingId: UUID,
    val tags: Set<String>,
    val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    val begrunnelser: List<Begrunnelse>,
    val saksbehandler: SaksbehandlerIdentOgNavn?,
    val beslutter: SaksbehandlerIdentOgNavn?,
    val automatiskFattet: Boolean,
    val dekning: Dekning?,
) : UtgåendeHendelse {
    sealed interface Sykepengegrunnlagsfakta

    data class FastsattEtterHovedregelSykepengegrunnlagsfakta(
        val omregnetÅrsinntekt: BigDecimal,
        val innrapportertÅrsinntekt: BigDecimal,
        val avviksprosent: BigDecimal,
        val seksG: BigDecimal,
        val tags: Set<String>,
        val arbeidsgivere: List<Arbeidsgiver>,
    ) : Sykepengegrunnlagsfakta {
        data class Arbeidsgiver(
            val organisasjonsnummer: String,
            val omregnetÅrsinntekt: BigDecimal,
            val innrapportertÅrsinntekt: BigDecimal,
        )
    }

    data class FastsattEtterSkjønnSykepengegrunnlagsfakta(
        val omregnetÅrsinntekt: BigDecimal,
        val innrapportertÅrsinntekt: BigDecimal,
        val avviksprosent: BigDecimal,
        val seksG: BigDecimal,
        val tags: Set<String>,
        val arbeidsgivere: List<Arbeidsgiver>,
        val skjønnsfastsettingtype: Skjønnsfastsettingstype,
        val skjønnsfastsettingsårsak: Skjønnsfastsettingsårsak,
        val skjønnsfastsatt: BigDecimal,
    ) : Sykepengegrunnlagsfakta {
        data class Arbeidsgiver(
            val organisasjonsnummer: String,
            val omregnetÅrsinntekt: BigDecimal,
            val innrapportertÅrsinntekt: BigDecimal,
            val skjønnsfastsatt: BigDecimal,
        )
    }

    data class FastsattIInfotrygdSykepengegrunnlagsfakta(
        val omregnetÅrsinntekt: BigDecimal,
    ) : Sykepengegrunnlagsfakta

    data class SelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
        val beregningsgrunnlag: BigDecimal,
        val tags: Set<String>,
        val seksG: BigDecimal,
        val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>,
    ) : Sykepengegrunnlagsfakta {
        data class PensjonsgivendeInntekt(
            val årstall: Int,
            val beløp: BigDecimal,
        )
    }

    data class Begrunnelse(
        val type: BegrunnelseType,
        val begrunnelse: String,
    )

    data class Dekning(
        val dekningsgrad: Int,
        val gjelderFraDag: Int,
    )

    enum class BegrunnelseType {
        SkjønnsfastsattSykepengegrunnlagMal,
        SkjønnsfastsattSykepengegrunnlagFritekst,
        SkjønnsfastsattSykepengegrunnlagKonklusjon,
        Avslag,
        DelvisInnvilgelse,
        Innvilgelse,
    }

    enum class Skjønnsfastsettingstype {
        OMREGNET_ÅRSINNTEKT,
        RAPPORTERT_ÅRSINNTEKT,
        ANNET,
    }

    enum class Skjønnsfastsettingsårsak {
        ANDRE_AVSNITT,
        TREDJE_AVSNITT,
    }
}
