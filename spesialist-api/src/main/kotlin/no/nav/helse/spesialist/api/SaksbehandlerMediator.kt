package no.nav.helse.spesialist.api

import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import org.slf4j.LoggerFactory

class SaksbehandlerMediator(
    dataSource: DataSource,
    private val rapidsConnection: RapidsConnection
) {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)

    fun håndter(annulleringDto: AnnulleringDto, saksbehandler: Saksbehandler) {
        tellAnnullering()
        saksbehandler.persister(saksbehandlerDao)

        val annulleringMessage = JsonMessage.newMessage("annullering", mutableMapOf(
            "fødselsnummer" to annulleringDto.fødselsnummer,
            "organisasjonsnummer" to annulleringDto.organisasjonsnummer,
            "aktørId" to annulleringDto.aktørId,
            "saksbehandler" to saksbehandler.json().toMutableMap()
                .apply { put("ident", annulleringDto.saksbehandlerIdent) },
            "fagsystemId" to annulleringDto.fagsystemId,
            "begrunnelser" to annulleringDto.begrunnelser,
        ).apply {
            compute("kommentar") { _, _ -> annulleringDto.kommentar }
        })

        rapidsConnection.publish(annulleringDto.fødselsnummer, annulleringMessage.toJson().also {
            sikkerlogg.info(
                "sender annullering for {}, {}\n\t$it",
                keyValue("fødselsnummer", annulleringDto.fødselsnummer),
                keyValue("organisasjonsnummer", annulleringDto.organisasjonsnummer)
            )
        })
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}