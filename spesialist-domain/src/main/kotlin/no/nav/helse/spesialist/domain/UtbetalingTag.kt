package no.nav.helse.spesialist.domain

enum class UtbetalingTag {
    Arbeidsgiverutbetaling,
    NegativArbeidsgiverutbetaling,
    Personutbetaling,
    NegativPersonutbetaling,
    IngenUtbetaling,
    ;

    companion object {
        @Deprecated("Bør flyttes inn i Behandling når kommandoløypa bruker nye domeneklasser. Innfører denne midlertidig for å strupe bruk av Utbetaling")
        fun List<String>.inneholderUtbetalingTilSykmeldt(): Boolean {
            val tags = this.mapNotNull { runCatching { enumValueOf<UtbetalingTag>(it) }.getOrNull() }
            check(tags.isNotEmpty()) { "Mangler tag som forteller noe om hvem som mottar utbetaling" }
            return NegativPersonutbetaling in tags || Personutbetaling in tags
        }
    }
}
