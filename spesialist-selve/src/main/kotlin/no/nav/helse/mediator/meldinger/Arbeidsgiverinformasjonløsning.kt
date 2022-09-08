package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class Arbeidsgiverinformasjonløsning(private val arbeidsgivere: List<ArbeidsgiverDto>) {
    internal fun opprett(arbeidsgiverDao: ArbeidsgiverDao) {
        arbeidsgivere.forEach {
            arbeidsgiverDao.insertArbeidsgiver(it.orgnummer, it.navn, it.bransjer)
        }
    }

    internal fun oppdater(arbeidsgiverDao: ArbeidsgiverDao) {
        arbeidsgivere.forEach {
            arbeidsgiverDao.updateNavn(it.orgnummer, it.navn)
            arbeidsgiverDao.updateBransjer(it.orgnummer, it.bransjer)
        }
    }

    internal fun harSvarForAlle(orgnumre: List<String>) = arbeidsgivere.map { it.orgnummer }.containsAll(orgnumre)

    internal class ArbeidsgiverRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val behov = "Arbeidsgiverinformasjon"

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "behov")
                        it.demandValue("@final", true)
                        it.demandAll("@behov", listOf(behov))
                        it.requireKey("contextId", "hendelseId", "@id")
                        it.requireKey("@løsning.$behov")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val løsning = packet["@løsning.$behov"]
            mediator.løsning(
                hendelseId,
                contextId,
                UUID.fromString(packet["@id"].asText()),
                Arbeidsgiverinformasjonløsning(
                    løsning.map { arbeidsgiver ->
                        ArbeidsgiverDto(
                            orgnummer = arbeidsgiver.path("orgnummer").asText(),
                            navn = arbeidsgiver.path("navn").asText(),
                            bransjer = arbeidsgiver.path("bransjer").map { it.asText() },
                        )
                    }
                ),
                context
            )
        }
    }

    internal data class ArbeidsgiverDto(
        val orgnummer: String,
        val navn: String,
        val bransjer: List<String>
    )
}
