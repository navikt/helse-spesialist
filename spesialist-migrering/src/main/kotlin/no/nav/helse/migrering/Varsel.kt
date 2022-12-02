package no.nav.helse.migrering

import java.time.LocalDateTime
import java.util.UUID

internal class Varsel(
    private val vedtaksperiodeId: UUID,
    private val melding: String,
    private val opprettet: LocalDateTime,
    private val id: UUID,
) {

    internal companion object {
        internal fun List<Varsel>.varslerFor(vedtaksperiodeId: UUID): List<Varsel> {
            return filter { it.vedtaksperiodeId == vedtaksperiodeId }
        }

        internal fun List<Varsel>.lagre(generasjonId: UUID, spesialistDao: SpesialistDao) {
            forEach { it.lagre(generasjonId, spesialistDao) }
        }

        internal fun List<Varsel>.sortert(): List<Varsel> {
            return sortedBy { it.opprettet }
        }

        internal fun MutableList<Varsel>.konsumer(tidspunkt: LocalDateTime): List<Varsel> {
            val konsumerteVarsler = this.takeWhile { it.erFør(tidspunkt) }
            this.removeAll(konsumerteVarsler)
            return konsumerteVarsler
        }
    }

    internal fun lagre(generasjonId: UUID, spesialistDao: SpesialistDao) {
        val (definisjonRef, varselkode) = spesialistDao.finnDefinisjonFor(melding)
        spesialistDao.lagreVarsel(generasjonId, definisjonRef, varselkode, id, vedtaksperiodeId, opprettet)
    }

    internal fun erFør(tidspunkt: LocalDateTime) = opprettet <= tidspunkt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Varsel

        if (vedtaksperiodeId != other.vedtaksperiodeId) return false
        if (melding != other.melding) return false
        if (opprettet != other.opprettet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + melding.hashCode()
        result = 31 * result + opprettet.hashCode()
        return result
    }

}