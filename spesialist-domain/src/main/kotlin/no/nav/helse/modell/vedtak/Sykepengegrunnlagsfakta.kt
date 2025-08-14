package no.nav.helse.modell.vedtak

sealed interface Sykepengegrunnlagsfakta {
    val omregnetÅrsinntekt: Double

    data class Infotrygd(
        override val omregnetÅrsinntekt: Double,
    ) : Sykepengegrunnlagsfakta

    sealed class Spleis : Sykepengegrunnlagsfakta {
        abstract val seksG: Double
        private val _tags: MutableSet<String> = mutableSetOf()
        abstract val arbeidsgivere: List<Arbeidsgiver>
        val tags get() = _tags.toList()

        fun leggTilTags(tagsForSykepengegrunnlagsfakta: Set<String>) {
            this._tags.addAll(tagsForSykepengegrunnlagsfakta)
        }

        data class EtterSkjønn(
            override val omregnetÅrsinntekt: Double,
            override val seksG: Double,
            override val arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
            val skjønnsfastsatt: Double,
        ) : Spleis()

        data class EtterHovedregel(
            override val omregnetÅrsinntekt: Double,
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

enum class Faktatype {
    ETTER_SKJØNN,
    ETTER_HOVEDREGEL,
    I_INFOTRYGD,
}
