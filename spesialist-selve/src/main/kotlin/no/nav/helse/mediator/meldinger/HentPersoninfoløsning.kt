package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.slf4j.LoggerFactory

internal class HentPersoninfoløsninger(private val løsninger: List<HentPersoninfoløsning>) {
    internal fun opprett(dao: ArbeidsgiverDao) {
        løsninger.forEach { it.lagre(dao) }
    }
}

internal class HentPersoninfoløsning(
    private val ident: String,
    private val fornavn: String,
    private val mellomnavn: String?,
    private val etternavn: String,
    private val fødselsdato: LocalDate,
    private val kjønn: Kjønn,
    private val adressebeskyttelse: Adressebeskyttelse
) {
    internal fun lagre(personDao: PersonDao): Long =
        personDao.insertPersoninfo(fornavn, mellomnavn, etternavn, fødselsdato, kjønn, adressebeskyttelse)

    internal fun lagre(dao: ArbeidsgiverDao) {
//        dao.insertArbeidsgiver(ident, "$fornavn $etternavn", listOf(BRANSJE_PRIVATPERSON))
        dao.updateOrInsertNavn(ident, "$fornavn $etternavn")
        dao.updateOrInsertBransjer(ident, listOf(BRANSJE_PRIVATPERSON))
    }

    internal fun oppdater(personDao: PersonDao, fødselsnummer: String) =
        personDao.updateOrInsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
            kjønn = kjønn,
            adressebeskyttelse = adressebeskyttelse
        )

    private companion object {
        private const val BRANSJE_PRIVATPERSON = "Privatperson"

        fun parsePersoninfo(node: JsonNode): HentPersoninfoløsning {
            val ident = node.path("ident").asText()
            val fornavn = node.path("fornavn").asText()
            val mellomnavn = node.path("mellomnavn").takeUnless(JsonNode::isMissingOrNull)?.asText()
            val etternavn = node.path("etternavn").asText()
            val fødselsdato = node.path("fødselsdato").asLocalDate()
            val kjønn = Kjønn.valueOf(node.path("kjønn").textValue())
            val adressebeskyttelse = Adressebeskyttelse.valueOf(node.path("adressebeskyttelse").textValue())
            return HentPersoninfoløsning(
                    ident,
                    fornavn,
                    mellomnavn,
                    etternavn,
                    fødselsdato,
                    kjønn,
                    adressebeskyttelse
            )
        }
    }
    internal class PersoninfoRiver(rapidsConnection: RapidsConnection, private val mediator: HendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {  validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentPersoninfoV2"))
                    it.demand("@løsning.HentPersoninfoV2") { require(it.isObject) }
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireKey("@løsning.HentPersoninfoV2.fornavn", "@løsning.HentPersoninfoV2.etternavn",
                        "@løsning.HentPersoninfoV2.fødselsdato", "@løsning.HentPersoninfoV2.kjønn", "@løsning.HentPersoninfoV2.adressebeskyttelse")
                    it.interestedIn("@løsning.HentPersoninfoV2.mellomnavn")
                }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke HentPersoninfoV2 (enkel):\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, UUID.fromString(packet["@id"].asText()), parsePersoninfo(packet["@løsning.HentPersoninfoV2"]), context)
        }
    }

    internal class FlerePersoninfoRiver(rapidsConnection: RapidsConnection, private val mediator: HendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {  validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentPersoninfoV2"))
                    it.demand("@løsning.HentPersoninfoV2") { require(it.isArray) }
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireArray("@løsning.HentPersoninfoV2") {
                        requireKey("ident", "fornavn", "etternavn", "fødselsdato", "kjønn", "adressebeskyttelse")
                        interestedIn("mellomnavn")
                    }
                }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke HentPersoninfoV2 (flere):\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, UUID.fromString(packet["@id"].asText()), HentPersoninfoløsninger(packet["@løsning.HentPersoninfoV2"].map { parsePersoninfo(it) }), context)
        }
    }
}
