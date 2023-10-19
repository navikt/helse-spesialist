package no.nav.helse.mediator.meldinger.løsninger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class DokumentRiver(
    rapidsConnection: RapidsConnection,
    private val dokumentDao: DokumentDao,
) : River.PacketListener {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "hent-dokument")
                    it.requireKey(
                        "@id",
                        "fødselsnummer",
                        "dokumentId",
                        "@løsning.dokument"
                    )
                }
            }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("forstod ikke hent-dokument:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val dokumentId = UUID.fromString(packet["dokumentId"].asText())
        val dokument = packet["@løsning.dokument"]

        sikkerLog.info(
            "Mottok hendelse hent-dokument og oppdaterer databasen for {} {}",
            StructuredArguments.kv("fødselsnummer", fødselsnummer),
            StructuredArguments.kv("dokumentId", dokumentId),
        )

        dokumentDao.lagre(fødselsnummer, dokumentId, dokument)
    }
}