package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Status
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        if (avviklet != other.avviklet) return false

        return true
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


    internal companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        internal fun List<Varseldefinisjon>.lagre(varselRepository: VarselRepository) {
            forEach { it.lagre(varselRepository) }
        }

        internal fun JsonNode.definisjoner(): List<Varseldefinisjon> {
            return this
                .map { jsonNode ->
                    Varseldefinisjon(
                        UUID.fromString(jsonNode["id"].asText()),
                        jsonNode["kode"].asText(),
                        jsonNode["tittel"].asText(),
                        jsonNode["forklaring"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                        jsonNode["handling"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                        jsonNode["avviklet"].asBoolean(),
                        jsonNode["opprettet"].asLocalDateTime()
                    )
                }
        }
    }

    internal class River(
        rapidsConnection: RapidsConnection,
        private val varselRepository: VarselRepository
    ) : PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "varseldefinisjoner_endret")
                    it.requireKey("@id")
                    it.requireArray("definisjoner") {
                        requireKey("id", "kode", "tittel", "avviklet")
                        interestedIn("forklaring", "handling")
                        require("opprettet", JsonNode::asLocalDateTime)
                    }
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke varseldefinisjoner_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding om varseldefinisjoner endret")

            val definisjoner = packet["definisjoner"].definisjoner()

            definisjoner.lagre(varselRepository)
        }
    }
}
