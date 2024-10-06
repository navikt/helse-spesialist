package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asYearMonth
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth

internal class Inntektløsning(
    private val inntekter: List<Inntekter>,
) {
    internal fun lagre(
        personRepository: PersonRepository,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): Long? = personRepository.lagreInntekter(fødselsnummer, skjæringstidspunkt, inntekter)

    internal class InntektRiver(
        private val mediator: MeldingMediator,
    ) : SpesialistRiver {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val behov = "InntekterForSykepengegrunnlag"

        override fun validations() =
            River.PacketValidation {
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

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
            sikkerLog.error("forstod ikke Inntekter:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            val hendelseId = packet["hendelseId"].asUUID()
            val contextId = packet["contextId"].asUUID()

            val inntektsløsning =
                Inntektløsning(
                    packet["@løsning.$behov"].map { løsning ->
                        Inntekter(
                            løsning["årMåned"].asYearMonth(),
                            løsning["inntektsliste"].map {
                                Inntekter.Inntekt(
                                    it["beløp"].asDouble(),
                                    it["orgnummer"].asText(),
                                )
                            },
                        )
                    },
                )

            mediator.løsning(hendelseId, contextId, packet["@id"].asUUID(), inntektsløsning, context)
        }
    }
}

data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
    data class Inntekt(val beløp: Double, val orgnummer: String)
}
