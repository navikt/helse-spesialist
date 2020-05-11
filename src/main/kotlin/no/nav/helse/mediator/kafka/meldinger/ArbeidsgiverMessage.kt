package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ArbeidsgiverMessage(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    val spleisbehovId: UUID,
    val arbeidsgiverNavn: String
) {
    fun asArbeidsgiverLøsning() =
        ArbeidsgiverLøsning(arbeidsgiverNavn)

    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandAll("@behov", Behovtype.HentArbeidsgiverNavn)
                    it.demandValue("final", true)
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                    it.requireKey("HentArbeidsgiverNavn")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = ArbeidsgiverMessage(
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asText(),
                spleisbehovId = UUID.fromString(packet["spleisBehovId"].asText()),
                arbeidsgiverNavn = packet["HentArbeidsgiverNavn"].asText()
            )

            spleisbehovMediator.håndter(behov.spleisbehovId, behov.asArbeidsgiverLøsning())
        }
    }
}
