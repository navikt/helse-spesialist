package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.modell.vedtaksperiode.vedtak.SykepengevedtakBuilder

internal class SkjønnsfastattSykepengegrunnlag(
    private val type: Skjønnsfastsettingstype,
    private val skjæringstidspunkt: LocalDate,
    private val begrunnelseFraMal: String,
    private val begrunnelseFraFritekst: String,
    private val begrunnelseFraKonklusjon: String,
    private val opprettet: LocalDateTime
) {
    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.skjønnsfastsettingData(begrunnelseFraMal, begrunnelseFraFritekst, begrunnelseFraKonklusjon, type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkjønnsfastattSykepengegrunnlag

        if (type != other.type) return false
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
        result = 31 * result + begrunnelseFraMal.hashCode()
        result = 31 * result + begrunnelseFraFritekst.hashCode()
        result = 31 * result + begrunnelseFraKonklusjon.hashCode()
        result = 31 * result + opprettet.hashCode()
        return result
    }

    internal companion object {
        internal fun List<SkjønnsfastattSykepengegrunnlag>.sortert() = sortedBy { it.opprettet }
    }
}

internal enum class Skjønnsfastsettingstype {
    OMREGNET_ÅRSINNTEKT,
    RAPPORTERT_ÅRSINNTEKT,
    ANNET,
}
