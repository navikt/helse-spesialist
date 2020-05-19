package no.nav.helse.modell.command

import net.logstash.logback.argument.StructuredArgument
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.VedtakDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class CommandExecutor(
    internal val command: RootCommand,
    private val spesialistOid: UUID,
    private val eventId: UUID,
    private val nåværendeOppgave: OppgaveDto?,
    private val oppgaveDao: OppgaveDao,
    private val vedtakDao: VedtakDao,
    private vararg val loggingData: StructuredArgument
) {
    private val log: Logger = LoggerFactory.getLogger("command")
    private fun Command.flat(): List<Command> = oppgaver.flatMap { it.flat() } + this

    private var nåværendeOppgavetype = nåværendeOppgave?.oppgaveType ?: command.flat().first().oppgavetype
    private val gjennståendeOppgaver = command
        .flat()
        .asSequence()
        .dropWhile { it.oppgavetype != nåværendeOppgavetype }

    private fun current(): Command = gjennståendeOppgaver.first { it.oppgavetype == nåværendeOppgavetype }

    internal fun currentTimeout() = current().timeout

    internal fun fortsett(løsning: HentEnhetLøsning) {
        current().fortsett(løsning)
    }

    internal fun fortsett(løsning: HentPersoninfoLøsning) {
        current().fortsett(løsning)
    }

    internal fun fortsett(løsning: HentInfotrygdutbetalingerLøsning) {
        current().fortsett(løsning)
    }

    internal fun fortsett(løsning: ArbeidsgiverLøsning) {
        current().fortsett(løsning)
    }

    internal fun fortsett(løsning: SaksbehandlerLøsning) {
        current().fortsett(løsning)
    }

    internal fun invalider() {
        oppgaveDao.updateOppgave(eventId, current().oppgavetype, Oppgavestatus.Invalidert, null, null)
    }

    internal fun execute(): List<Command.Resultat> {
        if (nåværendeOppgave?.status == Oppgavestatus.Invalidert) {
            log.info("Kan ikke gjenoppta oppgave da den er invalidert ${loggingData.format()}", *loggingData)
            return listOf(Command.Resultat.Invalidert)
        }

        val executedCommands = mutableListOf<CommandExecution>()

        gjennståendeOppgaver
            .onEach { nåværendeOppgavetype = it.oppgavetype }
            .map(::tryExecute)
            .onEach { executedCommands.add(it) }
            .takeWhile(CommandExecution::skalFortsette)
            .forEach {
                log.info(
                    "Oppgave ${it.command::class.simpleName} ble executed. Nåværende oppgave er $nåværendeOppgavetype, ${loggingData.format()}",
                    *loggingData
                )
            }

        val vedtakRef = command.vedtaksperiodeId?.let(vedtakDao::findVedtak)?.id
        val førsteCommand = executedCommands.first()
        val sisteCommand = executedCommands.last()
        oppgaveDao.updateOppgave(
            behovId = eventId,
            oppgavetype = førsteCommand.command.oppgavetype,
            oppgavestatus = førsteCommand.oppgavestatus(),
            ferdigstiltAv = førsteCommand.ferdigstiltAv(),
            oid = førsteCommand.oid()
        )


        if (nåværendeOppgave == null || executedCommands.size > 1) {
            oppgaveDao.insertOppgave(
                behovId = eventId,
                oppgavetype = sisteCommand.command.oppgavetype,
                oppgavestatus = sisteCommand.oppgavestatus(),
                ferdigstiltAv = sisteCommand.ferdigstiltAv(),
                oid = sisteCommand.oid(),
                vedtakRef = vedtakRef
            )
        } else {
            log.warn(
                "Execute av command førte ikke til endring i nåværende oppgavetype, ${loggingData.format()}",
                *loggingData
            )
        }

        if (sisteCommand is CommandExecution.Error) {
            throw sisteCommand.exception
        }

        log.info(
            "Oppgaver utført, gikk fra ${førsteCommand.command.oppgavetype} til ${sisteCommand.command.oppgavetype}, ${loggingData.format()}",
            *loggingData
        )

        return executedCommands.filterIsInstance<CommandExecution.Ok>().map { it.resultat }
    }

    private fun tryExecute(command: Command) = try {
        CommandExecution.Ok(
            command,
            spesialistOid,
            command.execute()
        )
    } catch (e: Exception) {
        CommandExecution.Error(command, e)
    }

    sealed class CommandExecution(val command: Command) {
        abstract fun skalFortsette(): Boolean
        abstract fun oid(): UUID?
        abstract fun ferdigstiltAv(): String?
        abstract fun oppgavestatus(): Oppgavestatus

        class Ok internal constructor(
            command: Command,
            private val spesialistOid: UUID,
            val resultat: Command.Resultat
        ) : CommandExecution(command) {
            override fun skalFortsette() = resultat is Command.Resultat.Ok
            override fun oid(): UUID? = when (resultat) {
                Command.Resultat.Ok.System -> spesialistOid
                is Command.Resultat.Ok.Løst -> resultat.oid
                else -> null
            }

            override fun ferdigstiltAv(): String? = when (resultat) {
                Command.Resultat.Ok.System -> "tbd@nav.no"
                is Command.Resultat.Ok.Løst -> resultat.ferdigstiltAv
                else -> null
            }

            override fun oppgavestatus(): Oppgavestatus = resultat.tilOppgavestatus()
        }

        class Error internal constructor(command: Command, val exception: Exception) : CommandExecution(command) {
            override fun skalFortsette(): Boolean = false
            override fun oid(): UUID? = null
            override fun ferdigstiltAv(): String? = null
            override fun oppgavestatus(): Oppgavestatus = Oppgavestatus.AvventerSystem
        }
    }

    private fun Array<out StructuredArgument>.format() = joinToString(", ") { "{}" }
}
