package no.nav.helse.modell.vedtak

import java.time.LocalDate
import java.time.LocalDateTime

data class SkjønnsfastsattSykepengegrunnlagDto(
    val type: SkjønnsfastsettingstypeDto,
    val årsak: SkjønnsfastsettingsårsakDto,
    val skjæringstidspunkt: LocalDate,
    val begrunnelseFraMal: String,
    val begrunnelseFraFritekst: String,
    val begrunnelseFraKonklusjon: String,
    val opprettet: LocalDateTime,
)

enum class SkjønnsfastsettingstypeDto {
    OMREGNET_ÅRSINNTEKT,
    RAPPORTERT_ÅRSINNTEKT,
    ANNET,
}

internal fun Skjønnsfastsettingstype.toDto() =
    when (this) {
        Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT
        Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsettingstypeDto.RAPPORTERT_ÅRSINNTEKT
        Skjønnsfastsettingstype.ANNET -> SkjønnsfastsettingstypeDto.ANNET
    }

enum class SkjønnsfastsettingsårsakDto {
    ANDRE_AVSNITT,
    TREDJE_AVSNITT,
}

internal fun Skjønnsfastsettingsårsak.toDto() =
    when (this) {
        Skjønnsfastsettingsårsak.ANDRE_AVSNITT -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
        Skjønnsfastsettingsårsak.TREDJE_AVSNITT -> SkjønnsfastsettingsårsakDto.TREDJE_AVSNITT
    }
