package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettMinimalArbeidsgiverCommand
import no.nav.helse.modell.kommando.OpprettMinimalPersonCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class SøknadSendt(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val json: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettMinimalPersonCommand(fødselsnummer, aktørId, personDao),
        OpprettMinimalArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao),
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson() = json

    internal class SøknadSendtRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandAny("@event_name", listOf("sendt_søknad_arbeidsgiver", "sendt_søknad_nav"))
                    it.requireKey(
                        "@id", "fnr", "aktorId", "arbeidsgiver.orgnummer"
                    )
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke SøknadSendt:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())

            if (!erProd()) {
                logg.info(
                    "Mottok SøknadSendt med {}",
                    keyValue("hendelseId", hendelseId)
                )
                sikkerLogg.info(
                    "Mottok SøknadSendt med {}, {}",
                    keyValue("hendelseId", hendelseId),
                    keyValue("hendelse", packet.toJson()),
                )
                mediator.søknadSendt(
                    message = packet,
                    id = hendelseId,
                    fødselsnummer = packet["fnr"].asText(),
                    aktørId = packet["aktorId"].asText(),
                    organisasjonsnummer = packet["arbeidsgiver.orgnummer"].asText(),
                    context = context
                )
            } else {
                logg.info("SøknadSendt melding er mottatt, på ${packet["@event_name"].asText()}, men videre håndtering er togglet av")
            }
        }
    private fun erProd() = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")

    }
}
