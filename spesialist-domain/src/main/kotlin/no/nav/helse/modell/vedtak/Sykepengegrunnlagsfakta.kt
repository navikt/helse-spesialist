package no.nav.helse.modell.vedtak

import java.math.BigDecimal

sealed interface Sykepengegrunnlagsfakta {
    data object Infotrygd : Sykepengegrunnlagsfakta

    sealed class Spleis : Sykepengegrunnlagsfakta {
        abstract val seksG: Double

        sealed class Arbeidstaker : Spleis() {
            abstract val arbeidsgivere: List<Arbeidsgiver>

            data class EtterSkjønn(
                override val seksG: Double,
                override val arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
            ) : Arbeidstaker()

            data class EtterHovedregel(
                override val seksG: Double,
                override val arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
                val sykepengegrunnlag: Double,
            ) : Arbeidstaker()
        }

        data class SelvstendigNæringsdrivende(
            val sykepengegrunnlag: Double,
            override val seksG: Double,
            val selvstendig: Selvstendig,
        ) : Spleis() {
            data class Selvstendig(
                val beregningsgrunnlag: BigDecimal,
            )
        }

        sealed interface Arbeidsgiver {
            val organisasjonsnummer: String
            val omregnetÅrsinntekt: Double
            val inntektskilde: Inntektskilde

            data class EtterSkjønn(
                override val organisasjonsnummer: String,
                override val omregnetÅrsinntekt: Double,
                override val inntektskilde: Inntektskilde,
                val skjønnsfastsatt: Double,
            ) : Arbeidsgiver

            data class EtterHovedregel(
                override val organisasjonsnummer: String,
                override val omregnetÅrsinntekt: Double,
                override val inntektskilde: Inntektskilde,
            ) : Arbeidsgiver

            enum class Inntektskilde {
                Arbeidsgiver,
                AOrdningen,
                Saksbehandler,
                Sigrun,
            }
        }
    }
}
