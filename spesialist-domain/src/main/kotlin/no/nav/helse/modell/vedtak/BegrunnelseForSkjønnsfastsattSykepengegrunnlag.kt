package no.nav.helse.modell.vedtak

import java.time.LocalDate
import java.time.LocalDateTime

data class BegrunnelseForSkjønnsfastsattSykepengegrunnlag(
    val type: Skjønnsfastsettingstype,
    val årsak: Skjønnsfastsettingsårsak,
    val skjæringstidspunkt: LocalDate,
    val begrunnelseFraMal: String,
    val begrunnelseFraFritekst: String,
    val begrunnelseFraKonklusjon: String,
    val opprettet: LocalDateTime,
)

enum class Skjønnsfastsettingstype {
    OMREGNET_ÅRSINNTEKT,
    RAPPORTERT_ÅRSINNTEKT,
    ANNET,
}

enum class Skjønnsfastsettingsårsak {
    ANDRE_AVSNITT,
    TREDJE_AVSNITT,
}
