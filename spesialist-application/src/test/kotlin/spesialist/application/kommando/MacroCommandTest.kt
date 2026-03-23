package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.kommando.MacroCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID

internal class MacroCommandTest {
    private val constants: MutableList<String> = mutableListOf()
    private var executeCount: Int = 0
    private var resumeCount: Int = 0

    private lateinit var context: CommandContext

    @BeforeEach
    fun beforeEach() {
        constants.clear()
        executeCount = 0
        resumeCount = 0
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Kommandoer utføres i rekkefølge`() {
        val command1 = command(execute = { constants.add("Kommando A"); true })
        val command2 = command(execute = { constants.add("Kommando B"); true })
        val macroCommand = command1 + command2
        assertTrue(macroCommand.execute(context))
        assertRekkefølge("Kommando A", "Kommando B")
        assertTrue(context.sti().isEmpty())
    }

    @Test
    fun `Kommandoer kan suspenderes`() {
        val command1 = command(execute = { constants.add("Kommando A"); false })
        val command2 = command(execute = { constants.add("Kommando B"); true })
        val macroCommand = command1 + command2
        assertFalse(macroCommand.execute(context))
        assertRekkefølge("Kommando A")
        assertEquals(listOf(0), context.sti())
    }

    @Test
    fun `Kommandoer kan fortsette`() {
        val command1 = command(
            execute = { constants.add("Kommando A Før"); false },
            resume = { constants.add("Kommando A Etter"); true }
        )
        val command2 = command(execute = { constants.add("Kommando B"); true })
        val macroCommand = command1 + command2
        macroCommand.execute(context)
        assertTrue(macroCommand.resume(context))
        assertRekkefølge("Kommando A Før", "Kommando A Etter", "Kommando B")
        assertTrue(context.sti().isEmpty())
    }

    @Test
    fun `Nestede makrokommandoer`() {
        val command1 = command(execute = { constants.add("A"); true })
        val macroCommand1 =
            command(
                execute = { constants.add("B før"); false },
                resume = { constants.add("B etter"); true }
            ) +
            command(
                execute = { constants.add("C før"); false },
                resume = { constants.add("C etter"); true }
            )
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.execute(context)
        assertEquals(listOf(1, 0), context.sti())
        macroCommand2.resume(context)
        assertEquals(listOf(1, 1), context.sti())
        macroCommand2.resume(context)
        assertTrue(context.sti().isEmpty())
        assertRekkefølge("A", "B før", "B etter", "C før", "C etter")
    }

    @Test
    fun `Restore av kommandohierkarki`() {
        val command1 = command(execute = { constants.add("A"); true })
        val macroCommand1 =
            command(
                execute = { constants.add("B før"); false },
                resume = { constants.add("B etter"); true }
            ) +
            command(
                execute = { constants.add("C før"); false },
                resume = { constants.add("C etter"); true }
            )
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.resume(CommandContext(UUID.randomUUID(), listOf(1, 0)))
        assertRekkefølge("B etter", "C før")
    }

    @Test
    fun `Execute etter restore medfører at execution starter fra begynnelsen`() {
        val command1 = command(execute = { constants.add("A"); true })
        val macroCommand1 =
            command(
                execute = { constants.add("B før"); false },
                resume = { constants.add("B etter"); true }
            ) +
            command(
                execute = { constants.add("C før"); false },
                resume = { constants.add("C etter"); true }
            )
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.execute(CommandContext(UUID.randomUUID(), listOf(1, 0)))
        assertRekkefølge("A", "B før")
    }

    @Test
    fun `Utfører ikke commands etter at context er ferdigstilt i execute`() {
        val macroCommand =
            command(
                execute = { this.ferdigstill(context) }
            ) +
            command(
                execute = { throw Exception() }
            )
        assertDoesNotThrow { macroCommand.execute(context) }
    }

    @Test
    fun `Utfører ikke commands etter at context er ferdigstilt i resume`() {
        val macroCommand =
            command(
                execute = { throw Exception() },
                resume = { this.ferdigstill(context) }
            ) +
            command(
                execute = { throw Exception() }
            )
        assertDoesNotThrow { macroCommand.resume(context) }
    }

    private fun assertRekkefølge(vararg konstanter: String) {
        assertEquals(konstanter.toList(), constants)
    }

    private operator fun Command.plus(other: Command): MacroCommand {
        return object : MacroCommand() {
            override val commands: List<Command> = listOf(this@plus, other)
        }
    }

    private fun command(execute: Command.(context: CommandContext) -> Boolean, resume: Command.(context: CommandContext) -> Boolean = { true }): Command {
        return object : Command {
            override fun execute(context: CommandContext): Boolean {
                executeCount += 1
                return execute(this, context)
            }

            override fun resume(context: CommandContext): Boolean {
                resumeCount += 1
                return resume(this, context)
            }

        }
    }
}
