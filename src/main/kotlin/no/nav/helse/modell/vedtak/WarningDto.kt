package no.nav.helse.modell.vedtak

import no.nav.helse.modell.VedtakDao

internal class WarningDto(
    private val melding: String,
    private val kilde: WarningKilde
) {
    internal companion object {
        fun meldinger(warnings: List<WarningDto>) = warnings.map { it.melding }

        fun lagre(vedtakDao: VedtakDao, warnings: List<WarningDto>, vedtakRef: Long) {
            warnings.forEach { it.lagre(vedtakDao, vedtakRef) }
        }
    }

    internal fun lagre(vedtakDao: VedtakDao, vedtakRef: Long) {
        if (melding.isBlank()) return
        vedtakDao.leggTilWarning(vedtakRef, melding, kilde)
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
