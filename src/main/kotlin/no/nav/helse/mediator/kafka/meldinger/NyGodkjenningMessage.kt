package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.MacroCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class NyGodkjenningMessage(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    private val warnings: List<String>,
    private val periodetype: Saksbehandleroppgavetype? = null,
    private val json: String,
    oppgaveDao: OppgaveDao,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    speilSnapshotRestClient: SpeilSnapshotRestClient
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf()

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class GodkjenningMessageRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandAll("@behov", listOf("Godkjenning"))
                    it.rejectKey("@løsning")
                    it.requireKey(
                        "@id", "fødselsnummer", "aktørId", "organisasjonsnummer", "vedtaksperiodeId", "periodeFom",
                        "periodeTom", "warnings"
                    )
                    it.interestedIn("periodetype")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            mediator.godkjenning(
                message = packet,
                id = UUID.fromString(packet["@id"].asText()),
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["periodeTom"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
                warnings = packet["warnings"].toWarnings(),
                periodetype = packet["periodetype"].takeUnless(JsonNode::isMissingOrNull)?.let { Saksbehandleroppgavetype.valueOf(it.asText()) },
                context = context
            )
        }

        private fun JsonNode.toWarnings() = this["aktiviteter"].map { it["melding"].asText() }
    }
}
