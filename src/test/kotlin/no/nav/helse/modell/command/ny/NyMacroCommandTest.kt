package no.nav.helse.modell.command.ny

import kotliquery.Session
import kotliquery.sessionOf
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class NyMacroCommandTest {
    val dataSource = setupDataSourceMedFlyway()

    @Test
    fun `utfører undercommands`() {
        val command1 = command()
        val command2 = command()
        val macro = macroOf(command1, command2)
        sessionOf(dataSource, returnGeneratedKey = true).use(macro::execute)

        assertEquals(1, command1.executions)
        assertEquals(1, command2.executions)
    }

    @Test
    fun `kan resume undercommands`() {
        val firstCommand = command()
        val suspendingCommand = command() { NyCommand.Resultat.AvventerSystem }
        val lastCommand = command()
        val macro = macroOf(firstCommand, suspendingCommand, lastCommand)

        sessionOf(dataSource, returnGeneratedKey = true).use(macro::execute)
        assertEquals(1, firstCommand.executions)
        assertEquals(1, suspendingCommand.executions)
        assertEquals(0, lastCommand.executions, "lastCommand skal ikke executes")

        sessionOf(dataSource, returnGeneratedKey = true).use(macro::resume)
        assertEquals(1, firstCommand.executions)
        assertEquals(1, suspendingCommand.executions)
        assertEquals(1, suspendingCommand.resumes, "suspendingCommand skal resumes")
        assertEquals(1, lastCommand.executions, "lastCommand skal executes etter resume")
    }

    @Test
    fun `kan resume flere undercommands`() {
        val firstCommand = command()
        val firstSuspendingCommand = command() { NyCommand.Resultat.AvventerSystem }
        val secondSuspendingCommand = command() { NyCommand.Resultat.AvventerSystem }
        val lastCommand = command()
        val macro = macroOf(firstCommand, firstSuspendingCommand, secondSuspendingCommand, lastCommand)

        sessionOf(dataSource, returnGeneratedKey = true).use(macro::execute)
        assertEquals(1, firstCommand.executions)
        assertEquals(1, firstSuspendingCommand.executions)
        assertEquals(0, secondSuspendingCommand.executions)
        assertEquals(0, lastCommand.executions, "lastCommand skal ikke executes")

        sessionOf(dataSource, returnGeneratedKey = true).use(macro::resume)
        assertEquals(1, firstCommand.executions)
        assertEquals(1, firstSuspendingCommand.executions)
        assertEquals(1, firstSuspendingCommand.resumes, "firstSuspendingCommand skal resumes")
        assertEquals(1, secondSuspendingCommand.executions)
        assertEquals(0, lastCommand.executions, "lastCommand skal ikke executes etter første resume")

        sessionOf(dataSource, returnGeneratedKey = true).use(macro::resume)
        assertEquals(1, firstCommand.executions)
        assertEquals(1, firstSuspendingCommand.executions)
        assertEquals(1, firstSuspendingCommand.resumes, "firstSuspendingCommand skal resumes")
        assertEquals(1, secondSuspendingCommand.executions)
        assertEquals(1, secondSuspendingCommand.resumes, "secondSuspendingCommand skal resumes ved andre resume")
        assertEquals(1, lastCommand.executions, "lastCommand skal executes etter andre resume")
    }

    fun macroOf(vararg commands: NyCommand) = object : NyMacroCommand() {
        override val commands = commands.asList()
        override val type = UUID.randomUUID().toString()
    }

    fun command(executeCallback: () -> NyCommand.Resultat = { NyCommand.Resultat.Ok }) =
        CountingCommand(executeCallback)

    class CountingCommand(
        private val executeCallback: () -> NyCommand.Resultat
    ) : NyCommand {
        override val type = UUID.randomUUID().toString()

        var executions = 0
        var resumes = 0

        override fun execute(session: Session): NyCommand.Resultat {
            executions++
            return executeCallback()
        }

        override fun resume(session: Session): NyCommand.Resultat {
            resumes++
            return NyCommand.Resultat.Ok
        }
    }
}
