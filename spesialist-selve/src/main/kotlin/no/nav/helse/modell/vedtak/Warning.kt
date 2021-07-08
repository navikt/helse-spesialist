package no.nav.helse.modell.vedtak

import no.nav.helse.arbeidsgiver.ArbeidsgiverDto
import no.nav.helse.modell.WarningDao
import no.nav.helse.person.SnapshotDto
import no.nav.helse.warningteller
import java.util.*

internal sealed class MaybeWarning
internal object NotAWarning: MaybeWarning()
internal class Warning internal constructor(private val melding: String, private val kilde: WarningKilde): MaybeWarning() {
    internal fun lagre(warningDao: WarningDao, vedtakRef: Long) = warningDao.leggTilWarning(vedtakRef, melding, kilde)
    internal fun dto() = WarningDto(melding, kilde)
    override fun equals(other: Any?) = other is Warning && this.kilde == other.kilde && this.melding == other.melding
    override fun hashCode(): Int  = 31 * melding.hashCode() + kilde.hashCode()
    override fun toString(): String = melding
    companion object {
        internal fun leggTilAdvarsel(tekst: String, vedtaksperiodeId: UUID, kilde: WarningKilde, dao: WarningDao) {
            when(val w = warning(tekst, kilde)) {
                is Warning -> {
                    dao.leggTilWarning(vedtaksperiodeId, w)
                    warningteller.labels("WARN", tekst).inc()
                }
            }
        }
        internal fun warning(melding: String, kilde: WarningKilde) = if (melding.isBlank()) NotAWarning else Warning(melding, kilde)
        fun meldinger(warnings: List<MaybeWarning>) = warnings.filterIsInstance<Warning>().map { it.toString() }
        fun lagre(warningDao: WarningDao, warnings: List<MaybeWarning>, vedtakRef: Long) = warnings.forEach { when(it) { is Warning -> it.lagre(warningDao, vedtakRef) } }
        internal fun warnings(vedtaksperiodeId: UUID, snapshot: SnapshotDto) = snapshot.arbeidsgivere.warnings(vedtaksperiodeId)
    }
}
private fun List<ArbeidsgiverDto>.warnings(vedtaksperiodeId: UUID) =
    this.flatMap { it.vedtaksperioder }
        .filter { UUID.fromString(it["id"].asText()) == vedtaksperiodeId }
        .flatMap { it.findValues("aktivitetslogg") }
        .flatten()
        .filter { it["alvorlighetsgrad"].asText() == "W" }
        .map { Warning(it["melding"].asText(), WarningKilde.Spleis) }

enum class WarningKilde { Spesialist, Spleis }
data class WarningDto(val melding: String, val kilde: WarningKilde)
