package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringMedResetCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class GosysOppgaveEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val utbetalingtype: Utbetalingtype,
    private val hendelseId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val json: String,
    private val godkjenningsbehovJson: String,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    warningDao: WarningDao,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
//        ÅpneGosysOppgaverCommand(
//            aktørId = aktørId,
//            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
//            warningDao = warningDao,
//            vedtaksperiodeId = vedtaksperiodeId
//        ),
//        AutomatiseringMedResetCommand(
//            fødselsnummer = fødselsnummer,
//            vedtaksperiodeId = vedtaksperiodeId,
//            utbetalingId = utbetalingId,
//            hendelseId = hendelseId,
//            automatisering = automatisering,
//            godkjenningsbehovJson = godkjenningsbehovJson,
//            utbetalingtype = utbetalingtype,
//            godkjenningMediator = godkjenningMediator,
//            periodeFom = periodeFom,
//            periodeTom = periodeTom
//        )
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : no.nav.helse.rapids_rivers.River.PacketListener {

        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "gosys_oppgave_endret")
                        it.requireKey("@id", "@opprettet", "fødselsnummer", "aktørId")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("Forstod ikke gosys_oppgave_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val fødselsnummer = packet["fødselsnummer"].asText()
            sikkerLog.info("Gosysoppgave med tema SYK endret for fnr {}", fødselsnummer)
            mediator.gosysOppgaveEndret(packet, context)
        }
    }

}