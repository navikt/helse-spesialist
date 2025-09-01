package no.nav.helse.modell.vedtak

sealed interface Sykepengegrunnlagsfakta {
    data object Infotrygd : Sykepengegrunnlagsfakta

    sealed class Spleis : Sykepengegrunnlagsfakta {
        abstract val seksG: Double
        abstract val arbeidsgivere: List<Arbeidsgiver>

        data class EtterSkjønn(
            override val seksG: Double,
            override val arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
        ) : Spleis()

        data class EtterHovedregel(
            override val seksG: Double,
            override val arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
            val sykepengegrunnlag: Double,
        ) : Spleis()

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
