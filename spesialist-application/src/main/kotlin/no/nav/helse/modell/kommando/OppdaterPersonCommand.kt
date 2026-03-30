package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.slf4j.LoggerFactory

internal class OppdaterPersonCommand(
    fødselsnummer: String,
    personRepository: PersonRepository,
) : MacroCommand() {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

    override val commands: List<Command> =
        listOf(
            OppdaterPersoninfoCommand(Identitetsnummer.fraString(fødselsnummer), personRepository, force = false),
            OppdaterEnhetCommand(fødselsnummer, personRepository),
            OppdaterInfotrygdutbetalingerCommand(),
        )

    private class OppdaterInfotrygdutbetalingerCommand : Command {
        override fun execute(
            context: CommandContext,
            sessionContext: SessionContext,
            outbox: Outbox,
        ): Boolean = ignorer()

        override fun resume(
            context: CommandContext,
            sessionContext: SessionContext,
            outbox: Outbox,
        ): Boolean = ignorer()

        private fun ignorer(): Boolean = true.also { log.info("Infotrygd-utbetalinger hentes ikke i kommandokjeden lenger") }
    }
}
