package no.nav.helse.modell.vedtaksperiode.vedtak

internal sealed class Sykepengegrunnlagsfakta(
    private val omregnetÅrsinntekt: Double,
) {
    internal class Infotrygd(omregnetÅrsinntekt: Double): Sykepengegrunnlagsfakta(omregnetÅrsinntekt)

    internal sealed class Spleis(
        omregnetArsinntekt: Double,
        private val innrapportertÅrsinntekt: Double,
        private val avviksprosent: Double,
        private val seksG: Int,
        private val tags: List<String>,
        private val arbeidsgivere: List<Arbeidsgiver>
    ): Sykepengegrunnlagsfakta(omregnetArsinntekt)

    internal class EtterSkjønn(
        omregnetÅrsinntekt: Double,
        innrapportertÅrsinntekt: Double,
        avviksprosent: Double,
        seksG: Int,
        private val skjønnsfastsatt: Double,
        tags: List<String>,
        arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>
    ): Spleis(
        omregnetÅrsinntekt,
        innrapportertÅrsinntekt,
        avviksprosent,
        seksG,
        tags,
        arbeidsgivere
    )

    internal class EtterHovedregel(
        omregnetÅrsinntekt: Double,
        innrapportertÅrsinntekt: Double,
        avviksprosent: Double,
        seksG: Int,
        tags: List<String>,
        arbeidsgivere: List<Arbeidsgiver.EtterHoveregel>
    ): Spleis(
        omregnetÅrsinntekt,
        innrapportertÅrsinntekt,
        avviksprosent,
        seksG,
        tags,
        arbeidsgivere
    )
}

internal enum class Faktatype {
    ETTER_SKJØNN,
    ETTER_HOVEDREGEL,
    I_INFOTRYGD
}

internal sealed class Arbeidsgiver(
    organisasjonsnummer: String,
    omregnetArsinntekt: Double,
) {
    internal class EtterSkjønn(
        organisasjonsnummer: String,
        omregnetArsinntekt: Double,
        private val skjønnsfastsatt: Double
    ): Arbeidsgiver(organisasjonsnummer, omregnetArsinntekt)

    internal class EtterHoveregel(
        organisasjonsnummer: String,
        omregnetArsinntekt: Double,
    ): Arbeidsgiver(organisasjonsnummer, omregnetArsinntekt)
}