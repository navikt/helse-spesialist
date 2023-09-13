package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asYearMonth
import org.slf4j.LoggerFactory

internal class Inntektløsning(
    private val inntekter: List<Inntekter>,
) {

    internal fun lagre(personDao: PersonDao, fødselsnummer: String, skjæringstidspunkt: LocalDate): Long? =
        personDao.insertInntekter(fødselsnummer, skjæringstidspunkt, inntekter)

    internal class InntektRiver(rapidsConnection: RapidsConnection, private val mediator: HendelseMediator) :
        River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val behov = "InntekterForSykepengegrunnlag"

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "behov")
                        it.demandValue("@final", true)
                        it.demandAll("@behov", listOf(behov))
                        it.demandKey("contextId")
                        it.demandKey("hendelseId")
                        it.requireKey("@id")
                        it.requireArray("@løsning.$behov") {
                            require("årMåned", JsonNode::asYearMonth)
                            requireArray("inntektsliste") {
                                requireKey("beløp")
                                interestedIn("orgnummer")
                            }
                        }
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke Inntekter:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())

            val inntektsløsning = Inntektløsning(
                packet["@løsning.$behov"].map { løsning ->
                    Inntekter(
                        løsning["årMåned"].asYearMonth(),
                        løsning["inntektsliste"].map {
                            Inntekter.Inntekt(
                                it["beløp"].asDouble(),
                                it["orgnummer"].asText(),
                            )
                        }
                    )
                }
            )

            mediator.løsning(hendelseId, contextId, UUID.fromString(packet["@id"].asText()), inntektsløsning, context)
        }
    }
}

data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
    data class Inntekt(val beløp: Double, val orgnummer: String)
}
