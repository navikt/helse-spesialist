package no.nav.helse.modell.vedtak

import no.nav.helse.modell.WarningDao
import no.nav.helse.person.SnapshotDto
import java.util.*

internal sealed class Warning (
    private val melding: String,
    private val kilde: WarningKilde
) {
    internal companion object {
        internal class EmptyWarning(private val kilde: WarningKilde): Warning("", kilde) {
            override fun equals(other: Any?): Boolean = other is EmptyWarning && this.kilde == other.kilde
            override fun hashCode(): Int = kilde.hashCode()
        }
        internal class ActualWarning internal constructor(private val melding: String, private val kilde: WarningKilde): Warning(melding, kilde) {
            override fun lagre(warningDao: WarningDao, vedtakRef: Long) {
                if (melding.isBlank()) return
                warningDao.leggTilWarning(vedtakRef, melding, kilde)
            }
            override fun equals(other: Any?) = other is ActualWarning && this.kilde == other.kilde && this.melding == other.melding
            override fun hashCode(): Int  = 31 * melding.hashCode() + kilde.hashCode()
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
    internal fun dto() = WarningDto(melding, kilde)
}

enum class WarningKilde { Spesialist, Spleis }
data class WarningDto(val melding: String, val kilde: WarningKilde)
