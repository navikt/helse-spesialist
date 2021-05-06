package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.util.*

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

    internal class ArbeidsgiverRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
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
