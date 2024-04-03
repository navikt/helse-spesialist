package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID

internal class Varseldefinisjon(
    private val id: UUID,
    private val varselkode: String,
    private val tittel: String,
    private val forklaring: String?,
    private val handling: String?,
    private val avviklet: Boolean,
    private val opprettet: LocalDateTime,
) {
    internal fun toDto() = VarseldefinisjonDto(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)

    override fun equals(other: Any?) =
        this === other || (
            other is Varseldefinisjon &&
                id == other.id &&
                varselkode == other.varselkode &&
                tittel == other.tittel &&
                forklaring == other.forklaring &&
                handling == other.handling &&
                avviklet == other.avviklet
        )

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + tittel.hashCode()
        result = 31 * result + (forklaring?.hashCode() ?: 0)
        result = 31 * result + (handling?.hashCode() ?: 0)
        result = 31 * result + avviklet.hashCode()
        return result
    }
}
