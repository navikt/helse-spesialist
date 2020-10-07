package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringCommand
import no.nav.helse.modell.command.nyny.*
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonCommand
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.risiko.RisikoCommand
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.*
import no.nav.helse.tildeling.ReservasjonDao
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
    warnings: List<String>,
    periodetype: Saksbehandleroppgavetype? = null,
    private val json: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    risikovurderingDao: RisikovurderingDao,
    reservasjonDao: ReservasjonDao,
    digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    oppgaveMediator: OppgaveMediator,
    miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    automatisering: Automatisering
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        KlargjørPersonCommand(fødselsnummer, aktørId, personDao),
        KlargjørArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao),
        KlargjørVedtaksperiodeCommand(
            speilSnapshotRestClient,
            id,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            periodeFom,
            periodeTom,
            warnings,
            periodetype,
            personDao,
            arbeidsgiverDao,
            snapshotDao,
            vedtakDao
        ),
        DigitalKontaktinformasjonCommand(
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao
        ),
        ÅpneGosysOppgaverCommand(
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao
        ),
        RisikoCommand(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            risikovurderingDao = risikovurderingDao,
            vedtakDao = vedtakDao,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
        ),
        AutomatiseringCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = id,
            automatisering = automatisering,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle,
            godkjenningsbehovJson = json
        ),
        SaksbehandlerGodkjenningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            godkjenningsbehovJson = json,
            reservasjonDao = reservasjonDao,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            hendelseId = id
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class GodkjenningMessageRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
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
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Mottok godkjenningsbehov med {}",
                keyValue("hendelseId", hendelseId)
            )
            sikkerLogg.info(
                "Mottok godkjenningsbehov med {}, {}",
                keyValue("hendelseId", hendelseId),
                keyValue("hendelse", packet.toJson())
            )
            mediator.godkjenning(
                message = packet,
                id = hendelseId,
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["periodeTom"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
                warnings = packet["warnings"].toWarnings(),
                periodetype = packet["periodetype"].takeUnless(JsonNode::isMissingOrNull)
                    ?.let { Saksbehandleroppgavetype.valueOf(it.asText()) },
                context = context
            )
        }

        private fun JsonNode.toWarnings() = this["aktiviteter"].map { it["melding"].asText() }
    }
}
