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
            val tags = utbetalingTags()
            return NegativPersonutbetaling in tags || Personutbetaling in tags
        }

        @Deprecated("Bør flyttes inn i Behandling når kommandoløypa bruker nye domeneklasser. Innfører denne midlertidig for å strupe bruk av Utbetaling")
        fun List<String>.kunUtbetalingTilArbeidsgiver(): Boolean {
            val tags = utbetalingTags()
            return tags.utbetalingTilArbeidsgiver() && !tags.utbetalingTilSykmeldt()
        }

        @Deprecated("Bør flyttes inn i Behandling når kommandoløypa bruker nye domeneklasser. Innfører denne midlertidig for å strupe bruk av Utbetaling")
        fun List<String>.kunUtbetalingTilSykmeldt(): Boolean {
            val tags = utbetalingTags()
            return tags.utbetalingTilSykmeldt() && !tags.utbetalingTilArbeidsgiver()
        }

        @Deprecated("Bør flyttes inn i Behandling når kommandoløypa bruker nye domeneklasser. Innfører denne midlertidig for å strupe bruk av Utbetaling")
        fun List<String>.deltUtbetaling(): Boolean {
            val tags = utbetalingTags()
            return tags.utbetalingTilSykmeldt() && tags.utbetalingTilArbeidsgiver()
        }

        @Deprecated("Bør flyttes inn i Behandling når kommandoløypa bruker nye domeneklasser. Innfører denne midlertidig for å strupe bruk av Utbetaling")
        fun List<String>.trekkesPenger(): Boolean {
            val tags = utbetalingTags()
            return NegativArbeidsgiverutbetaling in tags || NegativPersonutbetaling in tags
        }

        private fun List<UtbetalingTag>.utbetalingTilSykmeldt() = Personutbetaling in this || NegativPersonutbetaling in this

        private fun List<UtbetalingTag>.utbetalingTilArbeidsgiver() = Arbeidsgiverutbetaling in this || NegativArbeidsgiverutbetaling in this

        private fun List<String>.utbetalingTags(): List<UtbetalingTag> {
            val tags = this.mapNotNull { runCatching { enumValueOf<UtbetalingTag>(it) }.getOrNull() }
            check(tags.isNotEmpty()) { "Mangler tag som forteller noe om hvem som mottar utbetaling" }
            return tags
        }
    }
}
