package no.nav.helse.modell.vedtak

import no.nav.helse.modell.WarningDao

internal class WarningDto(
    private val melding: String,
    private val kilde: WarningKilde
) {
    internal companion object {
        fun meldinger(warnings: List<WarningDto>) = warnings.map { it.melding }

        fun lagre(warningDao: WarningDao, warnings: List<WarningDto>, vedtakRef: Long) {
            warnings.forEach { it.lagre(warningDao, vedtakRef) }
        }
    }

    internal fun lagre(warningDao: WarningDao, vedtakRef: Long) {
        if (melding.isBlank()) return
        warningDao.leggTilWarning(vedtakRef, melding, kilde)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is WarningDto) return false
        if (this === other) return true
        return this.melding == other.melding && this.kilde == other.kilde
    }

    override fun hashCode(): Int {
        var result = melding.hashCode()
        result = 31 * result + kilde.hashCode()
        return result
    }
}

enum class WarningKilde { Spesialist, Spleis }
