package no.nav.helse.modell.vedtak

sealed class Sykepengegrunnlagsfakta(
    val omregnetÅrsinntekt: Double,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is Sykepengegrunnlagsfakta &&
                this.omregnetÅrsinntekt == other.omregnetÅrsinntekt
        )

    override fun hashCode() = omregnetÅrsinntekt.hashCode()

    open fun medtags(tagsForSykepengegrunnlagsfakta: MutableSet<String>): Sykepengegrunnlagsfakta = this

    class Infotrygd(omregnetÅrsinntekt: Double) : Sykepengegrunnlagsfakta(omregnetÅrsinntekt)

    sealed class Spleis(
        omregnetÅrsinntekt: Double,
        val innrapportertÅrsinntekt: Double,
        val avviksprosent: Double,
        val seksG: Double,
        val tags: MutableSet<String>,
        val arbeidsgivere: List<Arbeidsgiver>,
    ) : Sykepengegrunnlagsfakta(omregnetÅrsinntekt) {
        override fun medtags(tagsForSykepengegrunnlagsfakta: MutableSet<String>): Sykepengegrunnlagsfakta {
            this.tags.addAll(tagsForSykepengegrunnlagsfakta)
            return this
        }

        override fun equals(other: Any?): Boolean =
            this === other || (
                super.equals(other) &&
                    other is Spleis &&
                    innrapportertÅrsinntekt == other.innrapportertÅrsinntekt &&
                    avviksprosent == other.avviksprosent &&
                    seksG == other.seksG &&
                    tags == other.tags &&
                    arbeidsgivere == other.arbeidsgivere
            )

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + innrapportertÅrsinntekt.hashCode()
            result = 31 * result + avviksprosent.hashCode()
            result = 31 * result + seksG.hashCode()
            result = 31 * result + tags.hashCode()
            result = 31 * result + arbeidsgivere.hashCode()
            return result
        }

        class EtterSkjønn(
            omregnetÅrsinntekt: Double,
            innrapportertÅrsinntekt: Double,
            avviksprosent: Double,
            seksG: Double,
            val skjønnsfastsatt: Double,
            tags: MutableSet<String>,
            arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
        ) : Spleis(
                omregnetÅrsinntekt,
                innrapportertÅrsinntekt,
                avviksprosent,
                seksG,
                tags,
                arbeidsgivere,
            ) {
            override fun equals(other: Any?) =
                this === other || (
                    super.equals(other) &&
                        other is EtterSkjønn &&
                        skjønnsfastsatt == other.skjønnsfastsatt
                )

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + skjønnsfastsatt.hashCode()
                return result
            }
        }

        @Suppress("EqualsOrHashCode")
        class EtterHovedregel(
            omregnetÅrsinntekt: Double,
            innrapportertÅrsinntekt: Double,
            avviksprosent: Double,
            seksG: Double,
            tags: MutableSet<String>,
            arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
        ) : Spleis(
                omregnetÅrsinntekt,
                innrapportertÅrsinntekt,
                avviksprosent,
                seksG,
                tags,
                arbeidsgivere,
            ) {
            override fun equals(other: Any?): Boolean = this === other || (super.equals(other) && other is EtterHovedregel)
        }

        sealed class Arbeidsgiver(
            val organisasjonsnummer: String,
            val omregnetÅrsinntekt: Double,
            val innrapportertÅrsinntekt: Double,
        ) {
            override fun equals(other: Any?): Boolean =
                this === other || (
                    other is Arbeidsgiver &&
                        organisasjonsnummer == other.organisasjonsnummer &&
                        omregnetÅrsinntekt == other.omregnetÅrsinntekt &&
                        innrapportertÅrsinntekt == other.innrapportertÅrsinntekt
                )

            override fun hashCode(): Int {
                var result = organisasjonsnummer.hashCode()
                result = 31 * result + omregnetÅrsinntekt.hashCode()
                result = 31 * result + innrapportertÅrsinntekt.hashCode()
                return result
            }

            class EtterSkjønn(
                organisasjonsnummer: String,
                omregnetÅrsinntekt: Double,
                innrapportertÅrsinntekt: Double,
                val skjønnsfastsatt: Double,
            ) : Arbeidsgiver(organisasjonsnummer, omregnetÅrsinntekt, innrapportertÅrsinntekt) {
                override fun equals(other: Any?) =
                    this === other || (
                        super.equals(other) &&
                            other is EtterSkjønn &&
                            skjønnsfastsatt == other.skjønnsfastsatt
                    )

                override fun hashCode(): Int {
                    var result = super.hashCode()
                    result = 31 * result + skjønnsfastsatt.hashCode()
                    return result
                }
            }

            @Suppress("EqualsOrHashCode")
            class EtterHovedregel(
                organisasjonsnummer: String,
                omregnetÅrsinntekt: Double,
                innrapportertÅrsinntekt: Double,
            ) : Arbeidsgiver(organisasjonsnummer, omregnetÅrsinntekt, innrapportertÅrsinntekt) {
                override fun equals(other: Any?) = this === other || (super.equals(other) && other is EtterHovedregel)
            }
        }
    }
}

enum class Faktatype {
    ETTER_SKJØNN,
    ETTER_HOVEDREGEL,
    I_INFOTRYGD,
}
