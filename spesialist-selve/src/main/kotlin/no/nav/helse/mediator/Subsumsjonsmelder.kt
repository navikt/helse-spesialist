package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.vilkårsprøving.SubsumsjonEvent
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class Subsumsjonsmelder(private val versjonAvKode: String, private val rapidsConnection: RapidsConnection) :
    SaksbehandlerObserver {
    private val versjon = "1.0.0"

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    override fun nySubsumsjon(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
    ) {
        val json = subsumsjonEvent.somJsonMessage().toJson()
        logg.info("Publiserer subsumsjon")
        sikkerlogg.info(
            "Publiserer subsumsjon for {}:\n{}",
            kv("fødselsnummer", fødselsnummer),
            kv("json", json),
        )
        rapidsConnection.publish(fødselsnummer, json)
    }

    private fun SubsumsjonEvent.somJsonMessage(): JsonMessage =
        JsonMessage.newMessage(
            "subsumsjon",
            mapOf(
                "subsumsjon" to
                    buildMap {
                        put("id", id)
                        put("versjon", versjon)
                        put("kilde", kilde)
                        put("versjonAvKode", versjonAvKode)
                        put("fodselsnummer", fødselsnummer)
                        put("sporing", sporing)
                        put("tidsstempel", tidsstempel)
                        put("lovverk", lovverk)
                        put("lovverksversjon", lovverksversjon)
                        put("paragraf", paragraf)
                        put("input", input)
                        put("output", output)
                        put("utfall", utfall)
                        ledd?.let { put("ledd", it) }
                        bokstav?.let { put("bokstav", it) }
                    },
            ),
        )
}
