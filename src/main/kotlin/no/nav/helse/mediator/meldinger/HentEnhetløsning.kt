package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class HentEnhetløsning(private val enhetNr: String) {

    companion object {
        private const val ENHET_UTLAND = "2101"
        internal fun erEnhetUtland(enhet: String) = enhet == ENHET_UTLAND
    }

    internal fun lagrePerson(
        personDao: PersonDao,
        fødselsnummer: String,
        aktørId: String,
        navnId: Int,
        infotrygdutbetalingerId: Int
    ) =
        personDao.insertPerson(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            navnId = navnId,
            enhetId = enhetNr.toInt(),
            infotrygdutbetalingerId = infotrygdutbetalingerId
        )

    internal fun tilhørerUtlandEnhet() = erEnhetUtland(enhetNr)

    fun oppdater(personDao: PersonDao, fødselsnummer: String) =
        personDao.updateEnhet(fødselsnummer, enhetNr.toInt())

    internal class HentEnhetRiver(rapidsConnection: RapidsConnection,
                                  private val mediator: IHendelseMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "behov")
                        it.demandValue("@final", true)
                        it.demandAll("@behov", listOf("HentEnhet"))
                        it.requireKey("@id", "contextId", "hendelseId", "@løsning.HentEnhet")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke HentEnhet:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, UUID.fromString(packet["@id"].asText()), HentEnhetløsning(packet["@løsning.HentEnhet"].asText()), context)
        }
    }
}
