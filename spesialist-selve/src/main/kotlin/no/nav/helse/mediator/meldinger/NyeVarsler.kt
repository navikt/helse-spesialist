package no.nav.helse.mediator.meldinger

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.NyeVarsler.Kontekst.Companion.vedtaksperiodeId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class NyeVarsler {

    data class Varsel(
        val id: UUID,
        val kode: String,
        val tittel: String,
        val forklaring: String,
        val handling: String,
        val avviklet: Boolean,
        val tidsstempel: LocalDateTime,
        val kontekster: List<Kontekst>
    ) {
        init {
            require(kontekster.vedtaksperiodeId() != null) {"varsel: ($kode, $id) er ikke i kontekst av en vedtaksperiode"}
        }
    }

    data class Kontekst(
        val konteksttype: String,
        val kontekstmap: Map<String, String>
    ) {

        companion object {
            internal fun List<Kontekst>.vedtaksperiodeId() =
                find { it.konteksttype == "Vedtaksperiode" }?.kontekstmap?.get("vedtaksperiodeId")
        }
    }

    internal class NyeVarslerRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {

        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireKey("@id")
                    it.demandValue("@event_name", "aktivitetslogg_nye_varsler")
                    it.demandKey("varsler")
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.demandKey("fødselsnummer")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Mottok aktivitetslogg_nye_varsler med {}",
                StructuredArguments.keyValue("hendelseId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok aktivitetslogg_nye_varsler med {}, {}",
                StructuredArguments.keyValue("hendelseId", hendelseId),
                StructuredArguments.keyValue("hendelse", packet.toJson()),
            )
            // mediator.nyeVarsler()
        }

    }
}