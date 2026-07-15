package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.spesialist.typer.Kjønn
import tools.jackson.databind.JsonNode

class PersoninfoløsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("HentPersoninfoV2"))
            it.require("@løsning.HentPersoninfoV2") { node -> require(node.isObject) }
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "contextId", "hendelseId")
            it.requireKey(
                "@løsning.HentPersoninfoV2.fornavn",
                "@løsning.HentPersoninfoV2.etternavn",
                "@løsning.HentPersoninfoV2.fødselsdato",
                "@løsning.HentPersoninfoV2.kjønn",
                "@løsning.HentPersoninfoV2.adressebeskyttelse",
            )
            it.interestedIn("@løsning.HentPersoninfoV2.mellomnavn")
            it.requireKey("sti")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = parsePersoninfo(packet["@løsning.HentPersoninfoV2"]),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
            sti = packet["sti"].toList().map { it.asInt() },
        )
    }
}

class FlerePersoninfoRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("HentPersoninfoV2"))
            it.require("@løsning.HentPersoninfoV2") { node -> require(node.isArray) }
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "contextId", "hendelseId")
            it.requireArray("@løsning.HentPersoninfoV2") {
                requireKey("ident", "fornavn", "etternavn", "fødselsdato", "kjønn", "adressebeskyttelse")
                interestedIn("mellomnavn")
            }
            it.requireKey("sti")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning =
                HentPersoninfoløsninger(
                    packet["@løsning.HentPersoninfoV2"].toList().map {
                        parsePersoninfo(
                            it,
                        )
                    },
                ),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
            sti = packet["sti"].toList().map { it.asInt() },
        )
    }
}

private fun parsePersoninfo(node: JsonNode): HentPersoninfoløsning {
    val ident = node.path("ident").asString()
    val fornavn = node.path("fornavn").asString()
    val mellomnavn = node.path("mellomnavn").takeUnless(JsonNode::isMissingOrNull)?.asString()
    val etternavn = node.path("etternavn").asString()
    val fødselsdato = node.path("fødselsdato").asLocalDate()
    val kjønn = Kjønn.valueOf(node.path("kjønn").textValue())
    val adressebeskyttelse = Adressebeskyttelse.valueOf(node.path("adressebeskyttelse").textValue())
    return HentPersoninfoløsning(ident, fornavn, mellomnavn, etternavn, fødselsdato, kjønn, adressebeskyttelse)
}
