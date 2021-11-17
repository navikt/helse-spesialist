package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.person.Kjønn
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class HentPersoninfoløsning(
    private val fornavn: String,
    private val mellomnavn: String?,
    private val etternavn: String,
    private val fødselsdato: LocalDate,
    private val kjønn: Kjønn
) {

    internal fun lagre(personDao: PersonDao): Long =
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
                    it.demandAll("@behov", listOf("HentPersoninfoV2"))
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireKey("@løsning.HentPersoninfoV2.fornavn", "@løsning.HentPersoninfoV2.etternavn",
                        "@løsning.HentPersoninfoV2.fødselsdato", "@løsning.HentPersoninfoV2.kjønn")
                    it.interestedIn("@løsning.HentPersoninfoV2.mellomnavn")
                }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke HentPersoninfoV2:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val fornavn = packet["@løsning.HentPersoninfoV2.fornavn"].asText()
            val mellomnavn = packet["@løsning.HentPersoninfoV2.mellomnavn"].takeUnless(JsonNode::isMissingOrNull)?.asText()
            val etternavn = packet["@løsning.HentPersoninfoV2.etternavn"].asText()
            val fødselsdato = packet["@løsning.HentPersoninfoV2.fødselsdato"].asLocalDate()
            val kjønn = Kjønn.valueOf(packet["@løsning.HentPersoninfoV2.kjønn"].textValue())
            mediator.løsning(hendelseId, contextId, UUID.fromString(packet["@id"].asText()), HentPersoninfoløsning(
                fornavn,
                mellomnavn,
                etternavn,
                fødselsdato,
                kjønn
            ), context)
        }
    }
}
