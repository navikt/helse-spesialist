package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class HentPersoninfoLøsning(
    private val fornavn: String,
    private val mellomnavn: String?,
    private val etternavn: String,
    private val fødselsdato: LocalDate,
    private val kjønn: Kjønn
) {

    internal fun lagre(personDao: PersonDao) =
        personDao.insertPersoninfo(fornavn, mellomnavn, etternavn, fødselsdato, kjønn)

    internal fun oppdater(personDao: PersonDao, fødselsnummer: String) =
        personDao.updatePersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
            kjønn = kjønn
        )

    internal class PersoninfoRiver(rapidsConnection: RapidsConnection, private val mediator: IHendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {  validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentPersoninfo"))
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireKey("@løsning.HentPersoninfo.fornavn", "@løsning.HentPersoninfo.etternavn",
                        "@løsning.HentPersoninfo.fødselsdato", "@løsning.HentPersoninfo.kjønn")
                    it.interestedIn("@løsning.HentPersoninfo.mellomnavn")
                }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke HentPersoninfo:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val fornavn = packet["@løsning.HentPersoninfo.fornavn"].asText()
            val mellomnavn = packet["@løsning.HentPersoninfo.mellomnavn"].takeUnless(JsonNode::isMissingOrNull)?.asText()
            val etternavn = packet["@løsning.HentPersoninfo.etternavn"].asText()
            val fødselsdato = packet["@løsning.HentPersoninfo.fødselsdato"].asLocalDate()
            val kjønn = Kjønn.valueOf(packet["@løsning.HentPersoninfo.kjønn"].textValue())
            mediator.løsning(hendelseId, contextId, UUID.fromString(packet["@id"].asText()), HentPersoninfoLøsning(
                fornavn,
                mellomnavn,
                etternavn,
                fødselsdato,
                kjønn
            ), context)
        }
    }
}

enum class Kjønn { Mann, Kvinne, Ukjent }
