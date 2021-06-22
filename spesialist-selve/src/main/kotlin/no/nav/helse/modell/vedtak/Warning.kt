package no.nav.helse.modell.vedtak

import no.nav.helse.modell.WarningDao
import no.nav.helse.person.SnapshotDto
import java.util.*

internal sealed class Warning (
    private val melding: String,
    private val kilde: WarningKilde
) {
    internal companion object {
        internal class EmptyWarning(kilde: WarningKilde): Warning("", kilde)
        internal class ActualWarning internal constructor(private val melding: String, private val kilde: WarningKilde): Warning(melding, kilde) {
            override fun lagre(warningDao: WarningDao, vedtakRef: Long) {
                if (melding.isBlank()) return
                warningDao.leggTilWarning(vedtakRef, melding, kilde)
            }
        }
        fun meldinger(warnings: List<Warning>) = warnings.map { it.melding }

        fun lagre(warningDao: WarningDao, warnings: List<Warning>, vedtakRef: Long) {
            warnings.forEach { it.lagre(warningDao, vedtakRef) }
        }
        internal fun warning(melding: String, kilde: WarningKilde) = if (melding.isBlank()) EmptyWarning(kilde) else ActualWarning(melding, kilde)

        internal fun warnings(vedtaksperiodeId: UUID, snapshot: SnapshotDto) =
            snapshot.arbeidsgivere
                .flatMap { it.vedtaksperioder }
                .filter { UUID.fromString(it["id"].asText()) == vedtaksperiodeId }
                .flatMap { it.findValues("aktivitetslogg") }
                .flatten()
                .filter { it["alvorlighetsgrad"].asText() == "W" }
                .map { ActualWarning(it["melding"].asText(), WarningKilde.Spleis) }
    }

    internal open fun lagre(warningDao: WarningDao, vedtakRef: Long) {}

    override fun equals(other: Any?): Boolean {
        if (other !is Warning) return false
        if (this === other) return true
        return this.melding == other.melding && this.kilde == other.kilde
    }

    override fun hashCode(): Int {
        var result = melding.hashCode()
        result = 31 * result + kilde.hashCode()
        return result
    }

    internal fun dto() = WarningDto(melding, kilde)
}

enum class WarningKilde { Spesialist, Spleis }
data class WarningDto(val melding: String, val kilde: WarningKilde)
