package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.abonnement.OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.fromNode
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.orgnummere
import no.nav.helse.modell.*
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SpeilSnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.command.KlargjørArbeidsforholdCommand
import no.nav.helse.modell.arbeidsforhold.command.SjekkArbeidsforholdCommand
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
import no.nav.helse.modell.opptegnelse.OpprettOpptegnelseCommand
import no.nav.helse.modell.opptegnelse.OpptegnelseDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikoCommand
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålCommand
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.modell.automatisering.AutomatiskAvvisningCommand
import no.nav.helse.oppgave.OppgaveMediator
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
    utbetalingId: UUID,
    arbeidsforholdId: String?,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    skjæringstidspunkt: LocalDate,
    periodetype: Periodetype,
    utbetalingtype: Utbetalingtype,
    inntektskilde: Inntektskilde,
    aktiveVedtaksperioder: List<AktivVedtaksperiode>,
    orgnummereMedAktiveArbeidsforhold: List<String>,
    private val json: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    warningDao: WarningDao,
    speilSnapshotDao: SpeilSnapshotDao,
    snapshotDao: SnapshotDao,
    commandContextDao: CommandContextDao,
    risikovurderingDao: RisikovurderingDao,
    digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    egenAnsattDao: EgenAnsattDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    vergemålDao: VergemålDao,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    speilSnapshotGraphQLClient: SpeilSnapshotGraphQLClient,
    oppgaveMediator: OppgaveMediator,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    opptegnelseDao: OpptegnelseDao,
    utbetalingDao: UtbetalingDao
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
            personDao = personDao
        ),
        KlargjørArbeidsgiverCommand(
            orgnummere = (aktiveVedtaksperioder.orgnummere() + orgnummereMedAktiveArbeidsforhold).distinct(),
            arbeidsgiverDao = arbeidsgiverDao
        ),
        KlargjørArbeidsforholdCommand(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            periodetype = periodetype
        ),
        KlargjørVedtaksperiodeCommand(
            speilSnapshotRestClient = speilSnapshotRestClient,
            speilSnapshotGraphQLClient = speilSnapshotGraphQLClient,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            vedtaksperiodetype = periodetype,
            inntektskilde = inntektskilde,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            speilSnapshotDao = speilSnapshotDao,
            snapshotDao = snapshotDao,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            utbetalingId = utbetalingId,
            utbetalingDao = utbetalingDao,
        ),
        EgenAnsattCommand(
            egenAnsattDao = egenAnsattDao,
        ),
        VergemålCommand(
            vergemålDao = vergemålDao,
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
        SjekkArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            arbeidsforholdId = arbeidsforholdId,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = periodetype,
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            warningDao = warningDao
        ),
        RisikoCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            aktiveVedtaksperioder = aktiveVedtaksperioder,
            risikovurderingDao = risikovurderingDao,
            warningDao = warningDao
        ),
        AutomatiskAvvisningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vergemålDao = vergemålDao,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator
        ),
        AutomatiseringCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            hendelseId = id,
            automatisering = automatisering,
            godkjenningsbehovJson = json,
            utbetalingtype = utbetalingtype,
            godkjenningMediator = godkjenningMediator
        ),
        OpprettSaksbehandleroppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            utbetalingtype = utbetalingtype,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            egenAnsattDao = egenAnsattDao,
            hendelseId = id,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao,
            vergemålDao = vergemålDao
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
                        "@id", "fødselsnummer", "aktørId", "organisasjonsnummer", "vedtaksperiodeId", "utbetalingId"
                    )
                    it.requireKey(
                        "Godkjenning.periodeFom",
                        "Godkjenning.periodeTom",
                        "Godkjenning.skjæringstidspunkt",
                        "Godkjenning.periodetype",
                        "Godkjenning.inntektskilde",
                        "Godkjenning.aktiveVedtaksperioder"
                    )
                    it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
                    it.interestedIn("Godkjenning.arbeidsforholdId", "Godkjenning.orgnummereMedAktiveArbeidsforhold")
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
                keyValue("hendelse", packet.toJson()),
            )
            mediator.godkjenningsbehov(
                message = packet,
                id = hendelseId,
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
                periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
                skjæringstidspunkt = LocalDate.parse(packet["Godkjenning.skjæringstidspunkt"].asText()),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
                utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
                arbeidsforholdId = packet["Godkjenning.arbeidsforholdId"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
                periodetype = Periodetype.valueOf(packet["Godkjenning.periodetype"].asText()),
                utbetalingtype = Utbetalingtype.valueOf(packet["Godkjenning.utbetalingtype"].asText()),
                inntektskilde = Inntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
                aktiveVedtaksperioder = fromNode(packet["Godkjenning.aktiveVedtaksperioder"]),
                orgnummereMedAktiveArbeidsforhold = packet["Godkjenning.orgnummereMedAktiveArbeidsforhold"]
                    .takeUnless(JsonNode::isMissingOrNull)
                    ?.map { it.asText() } ?: emptyList(),
                context = context
            )
        }
    }

    internal data class AktivVedtaksperiode(
        private val orgnummer: String,
        private val vedtaksperiodeId: UUID,
        private val periodetype: Periodetype
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
