package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.slf4j.LoggerFactory
import java.util.UUID

internal class PersoninfoRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) :
    River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentPersoninfoV2"))
                    it.demand("@løsning.HentPersoninfoV2") { require(it.isObject) }
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireKey(
                        "@løsning.HentPersoninfoV2.fornavn",
                        "@løsning.HentPersoninfoV2.etternavn",
                        "@løsning.HentPersoninfoV2.fødselsdato",
                        "@løsning.HentPersoninfoV2.kjønn",
                        "@løsning.HentPersoninfoV2.adressebeskyttelse",
                    )
                    it.interestedIn("@løsning.HentPersoninfoV2.mellomnavn")
                }
            }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLog.error("forstod ikke HentPersoninfoV2 (enkel):\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        mediator.løsning(
            hendelseId,
            contextId,
            UUID.fromString(packet["@id"].asText()),
            parsePersoninfo(packet["@løsning.HentPersoninfoV2"]),
            context,
        )
    }
}

internal class FlerePersoninfoRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) :
    River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentPersoninfoV2"))
                    it.demand("@løsning.HentPersoninfoV2") { require(it.isArray) }
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireArray("@løsning.HentPersoninfoV2") {
                        requireKey("ident", "fornavn", "etternavn", "fødselsdato", "kjønn", "adressebeskyttelse")
                        interestedIn("mellomnavn")
                    }
                }
            }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLog.error("forstod ikke HentPersoninfoV2 (flere):\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        mediator.løsning(
            hendelseId,
            contextId,
            UUID.fromString(packet["@id"].asText()),
            HentPersoninfoløsninger(
                packet["@løsning.HentPersoninfoV2"].map {
                    parsePersoninfo(
                        it,
                    )
                },
            ),
            context,
        )
    }
}

private fun parsePersoninfo(node: JsonNode): HentPersoninfoløsning {
    val ident = node.path("ident").asText()
    val fornavn = node.path("fornavn").asText()
    val mellomnavn = node.path("mellomnavn").takeUnless(JsonNode::isMissingOrNull)?.asText()
    val etternavn = node.path("etternavn").asText()
    val fødselsdato = node.path("fødselsdato").asLocalDate()
    val kjønn = Kjønn.valueOf(node.path("kjønn").textValue())
    val adressebeskyttelse = Adressebeskyttelse.valueOf(node.path("adressebeskyttelse").textValue())
    return HentPersoninfoløsning(ident, fornavn, mellomnavn, etternavn, fødselsdato, kjønn, adressebeskyttelse)
}
