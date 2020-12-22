package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class Arbeidsgiverinformasjonløsning(private val navn: String, private val bransjer: List<String>) {
    internal fun opprett(arbeidsgiverDao: ArbeidsgiverDao, orgnummer: String) {
        arbeidsgiverDao.insertArbeidsgiver(orgnummer, navn, bransjer)
    }

    internal fun oppdater(arbeidsgiverDao: ArbeidsgiverDao, orgnummer: String) {
        arbeidsgiverDao.updateNavn(orgnummer, navn)
        if (arbeidsgiverDao.findBransjerSistOppdatert(orgnummer) != null)
            arbeidsgiverDao.updateBransjer(orgnummer, bransjer)
        else
            arbeidsgiverDao.insertBransjer(orgnummer, bransjer)
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

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(
                hendelseId,
                contextId,
                UUID.fromString(packet["@id"].asText()),
                Arbeidsgiverinformasjonløsning(
                    navn = packet["@løsning.$behov"].path("navn").asText(),
                    bransjer = packet["@løsning.$behov"]
                        .path("bransjer")
                        .map { it.asText() }
                ),
                context
            )
        }
    }
}
