package no.nav.helse.modell.vedtak

sealed interface Sykepengegrunnlagsfakta {
    val omregnetÅrsinntekt: Double

    fun medtags(tagsForSykepengegrunnlagsfakta: MutableSet<String>): Sykepengegrunnlagsfakta = this

    data class Infotrygd(override val omregnetÅrsinntekt: Double) : Sykepengegrunnlagsfakta

    sealed interface Spleis : Sykepengegrunnlagsfakta {
        val innrapportertÅrsinntekt: Double
        val avviksprosent: Double
        val seksG: Double
        val tags: MutableSet<String>
        val arbeidsgivere: List<Arbeidsgiver>

        override fun medtags(tagsForSykepengegrunnlagsfakta: MutableSet<String>): Sykepengegrunnlagsfakta {
            this.tags.addAll(tagsForSykepengegrunnlagsfakta)
            return this
        }

        data class EtterSkjønn(
            override val omregnetÅrsinntekt: Double,
            override val innrapportertÅrsinntekt: Double,
            override val avviksprosent: Double,
            override val seksG: Double,
            val skjønnsfastsatt: Double,
            override val tags: MutableSet<String>,
            override val arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
        ) : Spleis

        data class EtterHovedregel(
            override val omregnetÅrsinntekt: Double,
            override val innrapportertÅrsinntekt: Double,
            override val avviksprosent: Double,
            override val seksG: Double,
            override val tags: MutableSet<String>,
            override val arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
        ) : Spleis

        sealed interface Arbeidsgiver {
            val organisasjonsnummer: String
            val omregnetÅrsinntekt: Double
            val innrapportertÅrsinntekt: Double

            data class EtterSkjønn(
                override val organisasjonsnummer: String,
                override val omregnetÅrsinntekt: Double,
                override val innrapportertÅrsinntekt: Double,
                val skjønnsfastsatt: Double,
            ) : Arbeidsgiver

            data class EtterHovedregel(
                override val organisasjonsnummer: String,
                override val omregnetÅrsinntekt: Double,
                override val innrapportertÅrsinntekt: Double,
            ) : Arbeidsgiver
        }
    }
}

enum class Faktatype {
    ETTER_SKJØNN,
    ETTER_HOVEDREGEL,
    I_INFOTRYGD,
}
