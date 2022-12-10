package no.nav.helse.modell.varsel

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class Varsel(
    private val id: UUID,
    private val varselkode: String,
    private val opprettet: LocalDateTime,
    private val vedtaksperiodeId: UUID,
    private var status: Status = AKTIV
) {

    internal enum class Status {
        AKTIV,
        INAKTIV,
        GODKJENT,
        AVVIST
    }

    internal fun lagre(varselRepository: VarselRepository) {
        varselRepository.lagreVarsel(id, varselkode, opprettet, vedtaksperiodeId)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Varsel
        && javaClass == other.javaClass
        && id == other.id
        && vedtaksperiodeId == other.vedtaksperiodeId
        && opprettet.withNano(0) == other.opprettet.withNano(0)
        && varselkode == other.varselkode)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + varselkode.hashCode()
        result = 31 * result + opprettet.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        return result
    }

    internal fun godkjennHvisAktiv(vedtaksperiodeId: UUID, ident: String, varselRepository: VarselRepository) {
        varselRepository.godkjennFor(vedtaksperiodeId, varselkode, ident, null)
    }

    internal fun avvisHvisAktiv(vedtaksperiodeId: UUID, ident: String, varselRepository: VarselRepository) {
        varselRepository.avvisFor(vedtaksperiodeId, varselkode, ident, null)
    }

    internal companion object {
        internal fun List<Varsel>.lagre(varselRepository: VarselRepository) {
            forEach { it.lagre(varselRepository) }
        }

        internal fun JsonNode.varsler(): List<Varsel> {
            return this
                .filter { it["nivå"].asText() == "VARSEL" && it["varselkode"]?.asText() != null }
                .filter { it["kontekster"].any { kontekst -> kontekst["konteksttype"].asText() == "Vedtaksperiode" } }
                .map { jsonNode ->
                    val vedtaksperiodeId =
                        UUID.fromString(
                            jsonNode["kontekster"]
                                .find { it["konteksttype"].asText() == "Vedtaksperiode" }!!["kontekstmap"]
                                .get("vedtaksperiodeId").asText()
                        )
                    Varsel(
                        UUID.fromString(jsonNode["id"].asText()),
                        jsonNode["varselkode"].asText(),
                        jsonNode["tidsstempel"].asLocalDateTime(),
                        vedtaksperiodeId
                    )
                }
        }
    }
}