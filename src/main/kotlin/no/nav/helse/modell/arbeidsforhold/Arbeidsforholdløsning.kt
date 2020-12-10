package no.nav.helse.modell.arbeidsforhold

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class Arbeidsforholdløsning(
    private val startdato: LocalDate,
    private val sluttdato: LocalDate?,
    private val stillingstittel: String,
    private val stillingsprosent: Int,
    private val organisasjonsnummer: String
) {
    internal fun lagre(arbeidsforholdDao: ArbeidsforholdDao, fødselsnummer: String): Long =
        arbeidsforholdDao.insertArbeidsforhold(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            startdato = startdato,
            sluttdato = sluttdato,
            stillingstittel = stillingstittel,
            stillingsprosent = stillingsprosent
        )

    internal fun oppdater(personDao: ArbeidsforholdDao, fødselsnummer: String) {
        personDao.oppdaterArbeidsforhold(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            startdato = startdato,
            sluttdato = sluttdato,
            stillingstittel = stillingstittel,
            stillingsprosent = stillingsprosent
        )
    }

    internal class ArbeidsforholdRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val behov = "Arbeidsgiverinformasjon"

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
                            "@løsning.$behov",
                            "@løsning.$behov.sluttdato"
                        )
                        message.require("@løsning.$behov.organisasjonsnummer") { it.asText() }
                        message.require("@løsning.$behov.startdato") { it.asLocalDate() }
                        message.require("@løsning.$behov.stillingstittel") { it.asInt() }
                        message.require("@løsning.$behov.stillingsprosent") { it.asText() }
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = packet.toArbeidsgiverløsning(),
                context = context
            )
        }

        private fun JsonMessage.toArbeidsgiverløsning() = Arbeidsforholdløsning(
            this["@løsning.$behov"].path("startdato").asLocalDate(),
            this["@løsning.$behov"].path("sluttdato").asOptionalLocalDate(),
            this["@løsning.$behov"].path("stillingstittel").asText(),
            this["@løsning.$behov"].path("stillingsprosent").asInt(),
            this["@løsning.$behov"].path("organisasjonsnummer").asText(),
        )
    }
}
