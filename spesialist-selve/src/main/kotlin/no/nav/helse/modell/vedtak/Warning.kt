package no.nav.helse.modell.vedtak

import no.nav.helse.modell.WarningDao
import no.nav.helse.person.SnapshotDto
import java.util.*

internal object EmptyWarning: Warning()
internal class ActualWarning internal constructor(private val melding: String, private val kilde: WarningKilde): Warning() {
    override fun lagre(warningDao: WarningDao, vedtakRef: Long) = warningDao.leggTilWarning(vedtakRef, melding, kilde)
    internal fun dto() = WarningDto(melding, kilde)
    override fun equals(other: Any?) = other is ActualWarning && this.kilde == other.kilde && this.melding == other.melding
    override fun hashCode(): Int  = 31 * melding.hashCode() + kilde.hashCode()
    override fun toString(): String = melding
}

internal sealed class Warning {
    internal companion object {
        fun meldinger(warnings: List<Warning>) = warnings.filter { it is ActualWarning}.map { it.toString() }
        fun lagre(warningDao: WarningDao, warnings: List<Warning>, vedtakRef: Long) = warnings.forEach { it.lagre(warningDao, vedtakRef) }
        internal fun warning(melding: String, kilde: WarningKilde) = if (melding.isBlank()) EmptyWarning else ActualWarning(melding, kilde)
        internal fun warnings(vedtaksperiodeId: UUID, snapshot: SnapshotDto) =
            snapshot.arbeidsgivere
                .flatMap { it.vedtaksperioder }
                .filter { UUID.fromString(it["id"].asText()) == vedtaksperiodeId }
                .flatMap { it.findValues("aktivitetslogg") }
                .flatten()
                .filter { it["alvorlighetsgrad"].asText() == "W" }
                .map { ActualWarning(it["melding"].asText(), WarningKilde.Spleis) }
    }
    internal open fun lagre(warningDao: WarningDao, vedtakRef: Long): Any = Unit
}

enum class WarningKilde { Spesialist, Spleis }
data class WarningDto(val melding: String, val kilde: WarningKilde)
