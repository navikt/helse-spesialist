package no.nav.helse.modell.person

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AdressebeskyttelseEndretRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger("adressebeskyttelse_endret_river")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "adressebeskyttelse_endret")
                it.requireKey("@id", "fødselsnummer")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        // Unngå å logge mer informasjon enn id her, vi ønsker ikke å lekke informasjon om adressebeskyttelse
        logg.info(
            "Mottok adressebeskyttelse_endret med {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
        )
        mediator.mottaMelding(melding = AdressebeskyttelseEndret(packet), messageContext = context)
    }
}
