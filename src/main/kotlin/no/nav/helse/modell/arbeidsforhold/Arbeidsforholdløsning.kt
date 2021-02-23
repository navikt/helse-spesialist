package no.nav.helse.modell.arbeidsforhold

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class Arbeidsforholdløsning(
    private val løsninger: List<Løsning>
) {

    data class Løsning(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
        val stillingstittel: String,
        val stillingsprosent: Int
    )

    internal fun opprett(
        arbeidsforholdDao: ArbeidsforholdDao,
        fødselsnummer: String,
        organisasjonsnummer: String
    ) =
        løsninger.forEach {
            arbeidsforholdDao.insertArbeidsforhold(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                startdato = it.startdato,
                sluttdato = it.sluttdato,
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent
            )
        }

    internal fun oppdater(personDao: ArbeidsforholdDao, fødselsnummer: String, organisasjonsnummer: String) {
        løsninger.forEach {
            personDao.oppdaterArbeidsforhold(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                startdato = it.startdato,
                sluttdato = it.sluttdato,
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent
            )
        }
    }

    internal class ArbeidsforholdRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val behov = "Arbeidsforhold"

        init {
            River(rapidsConnection)
                .apply {
                    validate { message ->
                        message.demandValue("@event_name", "behov")
                        message.demandValue("@final", true)
                        message.demandAll("@behov", listOf(behov))
                        message.requireKey(
                            "contextId",
                            "hendelseId",
                            "@id",
                            "@løsning.$behov"
                        )
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = packet.toArbeidsforholdløsninger(),
                context = context
            )
        }

        private fun JsonMessage.toArbeidsforholdløsninger(): Arbeidsforholdløsning =
            Arbeidsforholdløsning(this["@løsning.$behov"].map { it.toArbeidsforholdløsning() })

        private fun JsonNode.toArbeidsforholdløsning(): Løsning = Løsning(
            this["startdato"].asLocalDate(),
            this["sluttdato"].asOptionalLocalDate(),
            this["stillingstittel"].asText(),
            this["stillingsprosent"].asInt()
        )

    }
}
