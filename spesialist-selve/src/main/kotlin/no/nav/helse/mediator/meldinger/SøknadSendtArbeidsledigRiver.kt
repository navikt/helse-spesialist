import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class SøknadSendtArbeidsledigRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAny("@event_name", listOf("sendt_søknad_arbeidsledig"))
                it.forbid("arbeidsgiver.orgnummer")
                it.requireKey(
                    "@id", "fnr", "aktorId", "tidligereArbeidsgiverOrgnummer"
                )
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Forstod ikke SøknadSendt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())

        logg.info("Mottok SøknadSendt med {}", StructuredArguments.keyValue("hendelseId", hendelseId))
        sikkerLogg.info(
            "Mottok SøknadSendt med {}, {}",
            StructuredArguments.keyValue("hendelseId", hendelseId),
            StructuredArguments.keyValue("hendelse", packet.toJson()),
        )
        mediator.søknadSendt(
            message = packet,
            id = hendelseId,
            fødselsnummer = packet["fnr"].asText(),
            aktørId = packet["aktorId"].asText(),
            organisasjonsnummer = packet["tidligereArbeidsgiverOrgnummer"].asText(),
            context = context
        )
    }
}