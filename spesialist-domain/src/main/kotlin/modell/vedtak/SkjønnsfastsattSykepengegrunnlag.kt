package no.nav.helse.modell.vedtak

import java.time.LocalDate
import java.time.LocalDateTime

class SkjønnsfastsattSykepengegrunnlag(
    val type: Skjønnsfastsettingstype,
    val årsak: Skjønnsfastsettingsårsak,
    val skjæringstidspunkt: LocalDate,
    val begrunnelseFraMal: String,
    val begrunnelseFraFritekst: String,
    val begrunnelseFraKonklusjon: String,
    val opprettet: LocalDateTime,
) {
    fun toDto() =
        SkjønnsfastsattSykepengegrunnlagDto(
            type = type.toDto(),
            årsak = årsak.toDto(),
            skjæringstidspunkt = skjæringstidspunkt,
            begrunnelseFraMal = begrunnelseFraMal,
            begrunnelseFraFritekst = begrunnelseFraFritekst,
            begrunnelseFraKonklusjon = begrunnelseFraKonklusjon,
            opprettet = opprettet,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkjønnsfastsattSykepengegrunnlag

        if (type != other.type) return false
        if (årsak != other.årsak) return false
        if (skjæringstidspunkt != other.skjæringstidspunkt) return false
        if (begrunnelseFraMal != other.begrunnelseFraMal) return false
        if (begrunnelseFraFritekst != other.begrunnelseFraFritekst) return false
        if (begrunnelseFraKonklusjon != other.begrunnelseFraKonklusjon) return false
        if (opprettet.withNano(0) != other.opprettet.withNano(0)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = skjæringstidspunkt.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + årsak.hashCode()
        result = 31 * result + begrunnelseFraMal.hashCode()
        result = 31 * result + begrunnelseFraFritekst.hashCode()
        result = 31 * result + begrunnelseFraKonklusjon.hashCode()
        result = 31 * result + opprettet.hashCode()
        return result
    }

    companion object {
        fun gjenopprett(
            type: SkjønnsfastsettingstypeDto,
            årsak: SkjønnsfastsettingsårsakDto,
            skjæringstidspunkt: LocalDate,
            begrunnelseFraMal: String,
            begrunnelseFraFritekst: String,
            begrunnelseFraKonklusjon: String,
            opprettet: LocalDateTime,
        ) = SkjønnsfastsattSykepengegrunnlag(
            type = type.tilSkjønnsfastsettingtype(),
            årsak = årsak.tilSkjønnsfastsettingårsak(),
            skjæringstidspunkt = skjæringstidspunkt,
            begrunnelseFraMal = begrunnelseFraMal,
            begrunnelseFraFritekst = begrunnelseFraFritekst,
            begrunnelseFraKonklusjon = begrunnelseFraKonklusjon,
            opprettet = opprettet,
        )
    }
}

enum class Skjønnsfastsettingstype {
    OMREGNET_ÅRSINNTEKT,
    RAPPORTERT_ÅRSINNTEKT,
    ANNET,
}

internal fun SkjønnsfastsettingstypeDto.tilSkjønnsfastsettingtype() =
    when (this) {
        SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT -> Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
        SkjønnsfastsettingstypeDto.RAPPORTERT_ÅRSINNTEKT -> Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
        SkjønnsfastsettingstypeDto.ANNET -> Skjønnsfastsettingstype.ANNET
    }

enum class Skjønnsfastsettingsårsak {
    ANDRE_AVSNITT,
    TREDJE_AVSNITT,
}

internal fun SkjønnsfastsettingsårsakDto.tilSkjønnsfastsettingårsak() =
    when (this) {
        SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
        SkjønnsfastsettingsårsakDto.TREDJE_AVSNITT -> Skjønnsfastsettingsårsak.TREDJE_AVSNITT
    }
