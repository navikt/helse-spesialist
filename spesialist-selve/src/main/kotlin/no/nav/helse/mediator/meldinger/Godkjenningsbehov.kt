package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.command.KlargjørArbeidsforholdCommand
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringCommand
import no.nav.helse.modell.automatisering.AutomatiskAvvisningCommand
import no.nav.helse.modell.egenansatt.EgenAnsattCommand
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.KlargjørArbeidsgiverCommand
import no.nav.helse.modell.kommando.KlargjørPersonCommand
import no.nav.helse.modell.kommando.KlargjørVedtaksperiodeCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgaveCommand
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterPeriodehistorikkCommand
import no.nav.helse.modell.kommando.TrengerTotrinnsvurderingCommand
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikoCommand
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålCommand
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class Godkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    periodetype: Periodetype,
    førstegangsbehandling: Boolean,
    utbetalingtype: Utbetalingtype,
    inntektskilde: Inntektskilde,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
    skjæringstidspunkt: LocalDate,
    sykefraværstilfelle: Sykefraværstilfelle,
    private val json: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    warningDao: WarningDao,
    snapshotDao: SnapshotDao,
    commandContextDao: CommandContextDao,
    risikovurderingDao: RisikovurderingDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    egenAnsattDao: EgenAnsattDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    vergemålDao: VergemålDao,
    snapshotClient: SnapshotClient,
    oppgaveMediator: OppgaveMediator,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    utbetalingDao: UtbetalingDao,
    periodehistorikkDao: PeriodehistorikkDao,
    overstyringDao: OverstyringDao,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    snapshotMediator: SnapshotMediator,
) : Hendelse, MacroCommand() {

    // lambda fordi mange av testene ikke sørger for at utbetalingen fins i databasen før godkjenningsbehovet behandles
    private val utbetalingsfinner = { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) }
    private val utbetaling = utbetalingDao.hentUtbetaling(utbetalingId)

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
            orgnummere = orgnummereMedRelevanteArbeidsforhold + organisasjonsnummer,
            arbeidsgiverDao = arbeidsgiverDao
        ),
        KlargjørArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            førstegangsbehandling = førstegangsbehandling
        ),
        KlargjørVedtaksperiodeCommand(
            snapshotClient = snapshotClient,
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
            warningDao = warningDao,
            utbetalingId = utbetalingId,
            utbetalingDao = utbetalingDao,
        ),
        EgenAnsattCommand(
            egenAnsattDao = egenAnsattDao,
        ),
        VergemålCommand(
            vergemålDao = vergemålDao,
            warningDao = warningDao,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        ÅpneGosysOppgaverCommand(
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            warningDao = warningDao,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        RisikoCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            risikovurderingDao = risikovurderingDao,
            warningDao = warningDao,
            organisasjonsnummer = organisasjonsnummer,
            førstegangsbehandling = førstegangsbehandling,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetalingsfinner = utbetalingsfinner,
        ),
        AutomatiskAvvisningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vergemålDao = vergemålDao,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator,
            hendelseId = id,
            utbetaling = utbetaling
        ),
        AutomatiseringCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = id,
            automatisering = automatisering,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator,
            utbetaling = utbetaling,
            periodetype = periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
            periodeTom = periodeTom
        ),
        OpprettSaksbehandleroppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            hendelseId = id,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao,
            utbetalingId = utbetalingId,
            utbetalingtype = utbetalingtype,
            snapshotMediator = snapshotMediator,
        ),
        TrengerTotrinnsvurderingCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            overstyringDao = overstyringDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        PersisterPeriodehistorikkCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodehistorikkDao = periodehistorikkDao,
            utbetalingDao = utbetalingDao,
        ),
        PersisterInntektCommand(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            personDao = personDao
        ),
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class GodkjenningsbehovRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
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
                        "Godkjenning.førstegangsbehandling",
                        "Godkjenning.inntektskilde"
                    )
                    it.requireAny("Godkjenning.utbetalingtype", Utbetalingtype.gyldigeTyper.values())
                    it.interestedIn("Godkjenning.orgnummereMedRelevanteArbeidsforhold")
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
                periodetype = Periodetype.valueOf(packet["Godkjenning.periodetype"].asText()),
                førstegangsbehandling = packet["Godkjenning.førstegangsbehandling"].asBoolean(),
                utbetalingtype = Utbetalingtype.valueOf(packet["Godkjenning.utbetalingtype"].asText()),
                inntektskilde = Inntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
                orgnummereMedRelevanteArbeidsforhold = packet["Godkjenning.orgnummereMedRelevanteArbeidsforhold"]
                    .takeUnless(JsonNode::isMissingOrNull)
                    ?.map { it.asText() } ?: emptyList(),
                context = context
            )
        }
    }
}
