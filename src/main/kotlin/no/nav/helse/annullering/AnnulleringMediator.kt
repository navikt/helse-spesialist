package no.nav.helse.annullering

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.api.AnnulleringDto
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.*

internal class AnnulleringMediator(private val rapidsConnection: RapidsConnection) : IHendelseMediator {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun håndter(annulleringDto: AnnulleringDto, oid: UUID, epostadresse: String) {
        val annulleringMessage = annulleringDto.run {
            JsonMessage.newMessage(
                standardfelter("annuller", fødselsnummer).apply {
                    putAll(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "aktørId" to aktørId,
                            "saksbehandler" to oid,
                            "saksbehandlerEpost" to epostadresse,
                            "dager" to dager
                        )
                    )
                }
            )
        }

        rapidsConnection.publish(annulleringMessage.toJson().also {
            sikkerLogg.info(
                "sender annullering for {}, {}\n\t$it",
                keyValue("fødselsnummer", annulleringDto.fødselsnummer),
                keyValue("organisasjonsnummer", annulleringDto.organisasjonsnummer)
            )
        })
    }

    override fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
    }

    override fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
    }

    override fun løsning(hendelseId: UUID, contextId: UUID, løsning: Any, context: RapidsConnection.MessageContext) {}
}
