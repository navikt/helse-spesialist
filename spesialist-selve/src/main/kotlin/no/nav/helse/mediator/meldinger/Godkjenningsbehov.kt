package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.fromNode
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode.Companion.orgnummere
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
import no.nav.helse.modell.delvisRefusjon
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonCommand
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
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
import no.nav.helse.modell.kommando.PersisterPeriodehistorikkCommand
import no.nav.helse.modell.kommando.TrengerTotrinnsvurderingCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikoCommand
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsfilter
import no.nav.helse.modell.utbetaling.UtbetalingsfilterCommand
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.Companion.values
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålCommand
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
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
    utbetalingtype: Utbetalingtype,
    inntektskilde: Inntektskilde,
    aktiveVedtaksperioder: List<AktivVedtaksperiode>,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
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
    vergemålDao: VergemålDao,
    snapshotClient: SnapshotClient,
    oppgaveMediator: OppgaveMediator,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    utbetalingDao: UtbetalingDao,
    overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
    periodehistorikkDao: PeriodehistorikkDao,
) : Hendelse, MacroCommand() {
    private val utbetalingsfilter: () -> Utbetalingsfilter = {
        val utbetaling = snapshotDao.finnUtbetaling(
            fødselsnummer = fødselsnummer,
            utbetalingId = utbetalingId
        )
        Utbetalingsfilter(
            fødselsnummer = fødselsnummer,
            harUtbetalingTilSykmeldt = utbetaling.utbetalingTilSykmeldt(),
            delvisRefusjon = utbetaling.delvisRefusjon(),
            warnings = warningDao.finnAktiveWarnings(vedtaksperiodeId),
            periodetype = periodetype,
            inntektskilde = inntektskilde,
            utbetalingtype = utbetalingtype
        )
    }

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
            orgnummere = (aktiveVedtaksperioder.orgnummere() + orgnummereMedRelevanteArbeidsforhold).distinct(),
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
        UtbetalingsfilterCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            hendelseId = id,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator,
            utbetalingsfilter = utbetalingsfilter
        ),
        EgenAnsattCommand(
            egenAnsattDao = egenAnsattDao,
        ),
        VergemålCommand(
            vergemålDao = vergemålDao,
            warningDao = warningDao,
            vedtaksperiodeId = vedtaksperiodeId
        ),
        DigitalKontaktinformasjonCommand(
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao
        ),
        ÅpneGosysOppgaverCommand(
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            warningDao = warningDao,
            vedtaksperiodeId = vedtaksperiodeId
        ),
        RisikoCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            risikovurderingDao = risikovurderingDao,
            warningDao = warningDao,
            organisasjonsnummer = organisasjonsnummer,
            periodetype = periodetype
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
            utbetalingsfilter = utbetalingsfilter
        ),
        AutomatiseringCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            hendelseId = id,
            automatisering = automatisering,
            godkjenningsbehovJson = json,
            utbetalingtype = utbetalingtype,
            godkjenningMediator = godkjenningMediator,
        ),
        OpprettSaksbehandleroppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            hendelseId = id,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            utbetalingId = utbetalingId,
            utbetalingtype = utbetalingtype,
            snapshotDao = snapshotDao,
        ),
        TrengerTotrinnsvurderingCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            warningDao = warningDao,
            oppgaveMediator = oppgaveMediator,
            overstyrtVedtaksperiodeDao = overstyrtVedtaksperiodeDao
        ),
        PersisterPeriodehistorikkCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodehistorikkDao = periodehistorikkDao,
            utbetalingDao = utbetalingDao,
        )
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
                    it.interestedIn("Godkjenning.arbeidsforholdId", "Godkjenning.orgnummereMedRelevanteArbeidsforhold")
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
                orgnummereMedRelevanteArbeidsforhold = packet["Godkjenning.orgnummereMedRelevanteArbeidsforhold"]
                    .takeUnless(JsonNode::isMissingOrNull)
                    ?.map { it.asText() } ?: emptyList(),
                context = context
            )
        }
    }

    internal data class AktivVedtaksperiode(
        private val orgnummer: String,
        private val vedtaksperiodeId: UUID,
        private val periodetype: Periodetype) {

        companion object {
            internal fun List<AktivVedtaksperiode>.orgnummere() = map { it.orgnummer }

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
