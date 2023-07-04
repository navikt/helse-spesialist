package no.nav.helse.modell.vedtaksperiode.vedtak

internal sealed class Sykepengegrunnlagsfakta(
    private val omregnetÅrsinntekt: Double,
) {
    override fun equals(other: Any?): Boolean = this === other || (
        other is Sykepengegrunnlagsfakta
            && this.omregnetÅrsinntekt == other.omregnetÅrsinntekt
    )

    override fun hashCode() = omregnetÅrsinntekt.hashCode()

    internal class Infotrygd(omregnetÅrsinntekt: Double) : Sykepengegrunnlagsfakta(omregnetÅrsinntekt)

    internal sealed class Spleis(
        omregnetArsinntekt: Double,
        private val innrapportertÅrsinntekt: Double,
        private val avviksprosent: Double,
        private val seksG: Int,
        private val tags: List<String>,
        private val arbeidsgivere: List<Arbeidsgiver>,
    ) : Sykepengegrunnlagsfakta(omregnetArsinntekt) {
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
            result = 31 * result + seksG
            result = 31 * result + tags.hashCode()
            result = 31 * result + arbeidsgivere.hashCode()
            return result
        }

        internal class EtterSkjønn(
            omregnetÅrsinntekt: Double,
            innrapportertÅrsinntekt: Double,
            avviksprosent: Double,
            seksG: Int,
            private val skjønnsfastsatt: Double,
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

        internal class EtterHovedregel(
            omregnetÅrsinntekt: Double,
            innrapportertÅrsinntekt: Double,
            avviksprosent: Double,
            seksG: Int,
            tags: List<String>,
            arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
        ) : Spleis(
            omregnetÅrsinntekt,
            innrapportertÅrsinntekt,
            avviksprosent,
            seksG,
            tags,
            arbeidsgivere
        )

        internal sealed class Arbeidsgiver(
            private val organisasjonsnummer: String,
            private val omregnetArsinntekt: Double,
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
                private val skjønnsfastsatt: Double,
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

            internal class EtterHovedregel(
                organisasjonsnummer: String,
                omregnetArsinntekt: Double,
            ) : Arbeidsgiver(organisasjonsnummer, omregnetArsinntekt)
        }
    }
}

internal enum class Faktatype {
    ETTER_SKJØNN,
    ETTER_HOVEDREGEL,
    I_INFOTRYGD
}