package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Status

internal class Varseldefinisjon(
    private val id: UUID,
    private val varselkode: String,
    private val tittel: String,
    private val forklaring: String?,
    private val handling: String?,
    private val avviklet: Boolean,
    private val opprettet: LocalDateTime,
) {

    internal fun lagre(varselRepository: VarselRepository) {
        varselRepository.lagreDefinisjon(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Varseldefinisjon

        if (id != other.id) return false
        if (varselkode != other.varselkode) return false
        if (tittel != other.tittel) return false
        if (forklaring != other.forklaring) return false
        if (handling != other.handling) return false
        return avviklet == other.avviklet
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + tittel.hashCode()
        result = 31 * result + (forklaring?.hashCode() ?: 0)
        result = 31 * result + (handling?.hashCode() ?: 0)
        result = 31 * result + avviklet.hashCode()
        return result
    }

    fun oppdaterVarsel(
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
        status: Status,
        ident: String,
        oppdaterBlock: (vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, status: Status, ident: String, definisjonId: UUID) -> Unit,
    ) {
        oppdaterBlock(vedtaksperiodeId, generasjonId, varselkode, status, ident, this.id)
    }
}
