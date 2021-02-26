package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.fromNode
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.orgnummere
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.abonnement.OpprettOpptegnelseCommand
import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.abonnement.OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.command.KlargjørArbeidsforholdCommand
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringCommand
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonCommand
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattCommand
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.*
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikoCommand
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.SaksbehandlerInntektskilde
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class Godkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    periodetype: Saksbehandleroppgavetype,
    inntektskilde: SaksbehandlerInntektskilde,
    aktiveVedtaksperioder: List<AktivVedtaksperiode>,
    private val json: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    warningDao: WarningDao,
    snapshotDao: SnapshotDao,
    commandContextDao: CommandContextDao,
    risikovurderingDao: RisikovurderingDao,
    digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    egenAnsattDao: EgenAnsattDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    oppgaveMediator: OppgaveMediator,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    opptegnelseDao: OpptegnelseDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettKoblingTilHendelseCommand(
            hendelseId = id,
            vedtaksperiodeId = vedtaksperiodeId,
            vedtakDao = vedtakDao
        ),
        AvbrytContextCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            commandContextDao = commandContextDao
        ),
        KlargjørPersonCommand(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            personDao = personDao,
            godkjenningsbehovJson = json,
            vedtaksperiodeId = vedtaksperiodeId
        ),
        KlargjørArbeidsgiverCommand(
            orgnummere = aktiveVedtaksperioder.orgnummere(),
            arbeidsgiverDao = arbeidsgiverDao
        ),
        KlargjørArbeidsforholdCommand(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao
        ),
        KlargjørVedtaksperiodeCommand(
            speilSnapshotRestClient = speilSnapshotRestClient,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            vedtaksperiodetype = periodetype,
            inntektskilde = inntektskilde,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            snapshotDao = snapshotDao,
            vedtakDao = vedtakDao,
            warningDao = warningDao
        ),
        EgenAnsattCommand(
            egenAnsattDao = egenAnsattDao,
            godkjenningsbehovJson = json,
            vedtaksperiodeId = vedtaksperiodeId
        ),
        DigitalKontaktinformasjonCommand(
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            warningDao = warningDao,
            vedtaksperiodeId = vedtaksperiodeId
        ),
        ÅpneGosysOppgaverCommand(
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            warningDao = warningDao,
            vedtaksperiodeId = vedtaksperiodeId
        ),
        RisikoCommand(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            aktiveVedtaksperioder = aktiveVedtaksperioder,
            periodetype = periodetype,
            risikovurderingDao = risikovurderingDao,
            warningDao = warningDao
        ),
        AutomatiseringCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = id,
            automatisering = automatisering,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator
        ),
        OpprettSaksbehandleroppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            egenAnsattDao = egenAnsattDao,
            hendelseId = id,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao
        ),
        OpprettOpptegnelseCommand(
            opptegnelseDao = opptegnelseDao,
            fødselsnummer = fødselsnummer,
            hendelseId = id,
            opptegnelseType = NY_SAKSBEHANDLEROPPGAVE
        ),
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class GodkjenningsbehovRiver(
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
                        "@id", "fødselsnummer", "aktørId", "organisasjonsnummer", "vedtaksperiodeId"
                    )
                    it.requireKey(
                        "Godkjenning.periodeFom",
                        "Godkjenning.periodeTom",
                        "Godkjenning.periodetype",
                        "Godkjenning.inntektskilde",
                        "Godkjenning.aktiveVedtaksperioder"
                    )
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
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
            mediator.godkjenningsbehov(
                message = packet,
                id = hendelseId,
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
                periodetype = Saksbehandleroppgavetype.valueOf(packet["Godkjenning.periodetype"].asText()),
                inntektskilde = SaksbehandlerInntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
                aktiveVedtaksperioder = fromNode(packet["Godkjenning.aktiveVedtaksperioder"]),
                context = context
            )
        }
    }

    internal data class AktivVedtaksperiode(
        private val orgnummer: String,
        private val vedtaksperiodeId: UUID,
        private val periodetype: Saksbehandleroppgavetype
    ) {
        internal fun behov(context: CommandContext, vedtaksperiodeIdTilGodkjenning: UUID) {
            context.nyBehovgruppe()
            context.behov(
                "Risikovurdering", mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "organisasjonsnummer" to orgnummer,
                    "periodetype" to periodetype
                )
            )
            logg.info(
                "Trenger risikovurdering for {} (Periode til godkjenning: {})",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("vedtaksperiodeIdTilGodkjenning", vedtaksperiodeIdTilGodkjenning)
            )
        }

        companion object {
            private val logg = LoggerFactory.getLogger(AktivVedtaksperiode::class.java)

            internal fun List<AktivVedtaksperiode>.orgnummere() = map { it.orgnummer }

            internal fun List<AktivVedtaksperiode>.alleHarRisikovurdering(risikovurderingDao: RisikovurderingDao) =
                map { it.vedtaksperiodeId }
                    .all { vedtaksperiodeId -> risikovurderingDao.hentRisikovurdering(vedtaksperiodeId) != null }

            internal fun fromNode(json: JsonNode) = json.map {
                AktivVedtaksperiode(
                    orgnummer = it["orgnummer"].asText(),
                    vedtaksperiodeId = UUID.fromString(it["vedtaksperiodeId"].asText()),
                    periodetype = enumValueOf(it["periodetype"].asText())
                )
            }
        }
    }
}
