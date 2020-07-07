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

    @Test
    fun `kan resume macro med macroer`() {
        val firstCommand = command()
        val firstSuspendingCommand = command() { NyCommand.Resultat.AvventerSystem }
        val secondSuspendingCommand = command() { NyCommand.Resultat.AvventerSystem }
        val lastCommand = command()
        val subMacro = macroOf(firstCommand, firstSuspendingCommand, secondSuspendingCommand, lastCommand)
        val emptyMacro = macroOf()
        val macro = macroOf(emptyMacro, subMacro)

        sessionOf(dataSource, returnGeneratedKey = true).use(macro::execute)
        sessionOf(dataSource, returnGeneratedKey = true).use(macro::resume)
        sessionOf(dataSource, returnGeneratedKey = true).use(macro::resume)

        assertEquals(1, firstCommand.executions)
        assertEquals(1, firstSuspendingCommand.executions)
        assertEquals(1, firstSuspendingCommand.resumes)
        assertEquals(1, secondSuspendingCommand.executions)
        assertEquals(1, secondSuspendingCommand.resumes)
        assertEquals(1, lastCommand.executions)
    }

    @Test
    fun `kan resume macros med samme navn`() {
        val firstCommand1 = command("subCommand1") { NyCommand.Resultat.AvventerSystem }
        val secondCommand1 = command("subCommand2")
        val thirdCommand1 = command("subCommand3")
        val macro1 = macroOf(firstCommand1, secondCommand1, thirdCommand1, type = "macro")

        val firstCommand2 = command("subCommand1")
        val secondCommand2 = command("subCommand2") { NyCommand.Resultat.AvventerSystem }
        val thirdCommand2 = command("subCommand3")
        val macro2 = macroOf(firstCommand2, secondCommand2, thirdCommand2, type = "macro")

        sessionOf(dataSource, returnGeneratedKey = true).use(macro1::execute)
        sessionOf(dataSource, returnGeneratedKey = true).use(macro2::execute)

        sessionOf(dataSource, returnGeneratedKey = true).use(macro1::resume)
        sessionOf(dataSource, returnGeneratedKey = true).use(macro2::resume)

        assertEquals(1, firstCommand1.executions)
        assertEquals(1, firstCommand1.resumes)
        assertEquals(1, secondCommand1.executions)
        assertEquals(1, thirdCommand1.executions)

        assertEquals(1, firstCommand2.executions)
        assertEquals(1, secondCommand2.executions) { "secondCommand i andre macro skal bare bli executed en gang, før den resumes"}
        assertEquals(1, secondCommand2.resumes)
        assertEquals(1, thirdCommand2.executions)
    }

    private fun macroOf(
        vararg commands: NyCommand,
        type: String = UUID.randomUUID().toString()
    ) = NyMacroCommand(
        commands = commands.asList(),
        type = type,
        id = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use {
            it.persisterCommand(type = type, parent = null)
        })
    )

    private fun command(
        type: String = UUID.randomUUID().toString(),
        executeCallback: () -> NyCommand.Resultat = { NyCommand.Resultat.Ok }
    ) =
        CountingCommand(type, executeCallback)

    class CountingCommand(
        override val type: String,
        private val executeCallback: () -> NyCommand.Resultat
    ) : NyCommand {

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
