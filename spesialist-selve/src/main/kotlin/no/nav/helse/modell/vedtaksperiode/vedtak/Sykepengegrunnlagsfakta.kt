package no.nav.helse.modell.vedtaksperiode.vedtak

internal sealed class Sykepengegrunnlagsfakta(
    val omregnetÅrsinntekt: Double,
) {
    override fun equals(other: Any?): Boolean = this === other || (
        other is Sykepengegrunnlagsfakta
            && this.omregnetÅrsinntekt == other.omregnetÅrsinntekt
    )

    override fun hashCode() = omregnetÅrsinntekt.hashCode()

    internal class Infotrygd(omregnetÅrsinntekt: Double) : Sykepengegrunnlagsfakta(omregnetÅrsinntekt)

    internal sealed class Spleis(
        omregnetÅrsinntekt: Double,
        val innrapportertÅrsinntekt: Double,
        val avviksprosent: Double,
        val seksG: Double,
        val tags: List<String>,
        val arbeidsgivere: List<Arbeidsgiver>,
    ) : Sykepengegrunnlagsfakta(omregnetÅrsinntekt) {
        override fun equals(other: Any?): Boolean = this === other || (
            super.equals(other)
                && other is Spleis
                && innrapportertÅrsinntekt == other.innrapportertÅrsinntekt
                && avviksprosent == other.avviksprosent
                && seksG == other.seksG
                && tags == other.tags
                && arbeidsgivere == other.arbeidsgivere
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

        internal class EtterSkjønn(
            omregnetÅrsinntekt: Double,
            innrapportertÅrsinntekt: Double,
            avviksprosent: Double,
            seksG: Double,
            val skjønnsfastsatt: Double,
            tags: List<String>,
            arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
        ) : Spleis(
            omregnetÅrsinntekt,
            innrapportertÅrsinntekt,
            avviksprosent,
            seksG,
            tags,
            arbeidsgivere
        ) {
            override fun equals(other: Any?) = this === other || (
                super.equals(other)
                    && other is EtterSkjønn
                    && skjønnsfastsatt == other.skjønnsfastsatt
            )

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + skjønnsfastsatt.hashCode()
                return result
            }
        }

        @Suppress("EqualsOrHashCode")
        internal class EtterHovedregel(
            omregnetÅrsinntekt: Double,
            innrapportertÅrsinntekt: Double,
            avviksprosent: Double,
            seksG: Double,
            tags: List<String>,
            arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
        ) : Spleis(
            omregnetÅrsinntekt,
            innrapportertÅrsinntekt,
            avviksprosent,
            seksG,
            tags,
            arbeidsgivere
        ) {
            override fun equals(other: Any?): Boolean =
                this === other || (super.equals(other) && other is EtterHovedregel)
        }

        internal sealed class Arbeidsgiver(
            val organisasjonsnummer: String,
            val omregnetArsinntekt: Double,
        ) {
            override fun equals(other: Any?): Boolean = this === other || (
                    other is Arbeidsgiver
                            && organisasjonsnummer == other.organisasjonsnummer
                            && omregnetArsinntekt == other.omregnetArsinntekt
                    )

            override fun hashCode(): Int {
                var result = organisasjonsnummer.hashCode()
                result = 31 * result + omregnetArsinntekt.hashCode()
                return result
            }

            internal class EtterSkjønn(
                organisasjonsnummer: String,
                omregnetArsinntekt: Double,
                val skjønnsfastsatt: Double,
            ) : Arbeidsgiver(organisasjonsnummer, omregnetArsinntekt) {
                override fun equals(other: Any?) = this === other || (
                        super.equals(other)
                                && other is EtterSkjønn
                                && skjønnsfastsatt == other.skjønnsfastsatt
                        )

                override fun hashCode(): Int {
                    var result = super.hashCode()
                    result = 31 * result + skjønnsfastsatt.hashCode()
                    return result
                }
            }

            @Suppress("EqualsOrHashCode")
            internal class EtterHovedregel(
                organisasjonsnummer: String,
                omregnetArsinntekt: Double,
            ) : Arbeidsgiver(organisasjonsnummer, omregnetArsinntekt) {
                override fun equals(other: Any?) =
                    this === other || (super.equals(other) && other is EtterHovedregel)
            }
        }
    }
}

internal enum class Faktatype {
    ETTER_SKJØNN,
    ETTER_HOVEDREGEL,
    I_INFOTRYGD
}