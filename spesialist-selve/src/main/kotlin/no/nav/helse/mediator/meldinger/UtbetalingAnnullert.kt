package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.*
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.LagreAnnulleringCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotUtenÅLagreWarningsCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.saksbehandler.SaksbehandlerDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingAnnullert(
    override val id: UUID,
    private val fødselsnummer: String,
    utbetalingId: UUID,
    annullertTidspunkt: LocalDateTime,
    saksbehandlerEpost: String,
    private val json: String,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    snapshotDao: SnapshotDao,
    utbetalingDao: UtbetalingDao,
    saksbehandlerDao: SaksbehandlerDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotUtenÅLagreWarningsCommand(
            speilSnapshotRestClient = speilSnapshotRestClient,
            snapshotDao = snapshotDao,
            fødselsnummer = fødselsnummer
        ),
        LagreAnnulleringCommand(
            utbetalingDao = utbetalingDao,
            saksbehandlerDao = saksbehandlerDao,
            annullertTidspunkt = annullertTidspunkt,
            saksbehandlerEpost = saksbehandlerEpost,
            utbetalingId = utbetalingId
        )
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    internal class UtbetalingAnnullertRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : PacketListener {
        private val log = LoggerFactory.getLogger("UtbetalingAnnullert")
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "utbetaling_annullert")
                    it.requireKey(
                        "@id",
                        "fødselsnummer",
                        "utbetalingId",
                        "tidspunkt",
                        "epost"
                    )
                    it.interestedIn("arbeidsgiverFagsystemId", "personFagsystemId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke utbetaling_annullert:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val arbeidsgiverFagsystemId = packet["arbeidsgiverFagsystemId"].takeUnless { it.isMissingOrNull() }?.asText()
            val personFagsystemId = packet["personFagsystemId"].takeUnless { it.isMissingOrNull() }?.asText()

            val logInfo = mutableListOf(keyValue("eventId", id)).also {
                if (arbeidsgiverFagsystemId != null) it.add(keyValue("arbeidsgiverFagsystemId", arbeidsgiverFagsystemId))
                if (personFagsystemId != null) it.add(keyValue("personFagsystemId", personFagsystemId))
            }

            log.info(
                "Mottok utbetaling annullert ${logInfo.joinToString(transform = {"{}"})}",
                *logInfo.toTypedArray()
            )
            mediator.utbetalingAnnullert(packet, context)
        }
    }
}
