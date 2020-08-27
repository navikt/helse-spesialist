package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class MacroCommandTest {
    private val constants: MutableList<String> = mutableListOf()
    private var executeCount: Int = 0
    private var resumeCount: Int = 0
    private var undoCount: Int = 0

    private lateinit var context: CommandContext

    @BeforeEach
    fun beforeEach() {
        constants.clear()
        executeCount = 0
        resumeCount = 0
        undoCount = 0
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
    fun `Undo gjør ingenting dersom ingen kommandoer er kjørt`() {
        val macroCommand =
            command(
                execute = { constants.add("B før"); true },
                undo = { constants.add("B etter") }
            ) +
            command(
                execute = { constants.add("C før"); true },
                undo = { constants.add("C etter") }
            )
        macroCommand.undo(context)
        assertRekkefølge()
        assertTellere(0, 0, 0)
    }

    @Test
    fun `Undo reverserer alle kommandoer`() {
        val macroCommand =
            command(
                execute = { constants.add("B før"); true },
                undo = { constants.add("B etter") }
            ) +
            command(
                execute = { constants.add("C før"); true },
                undo = { constants.add("C etter") }
            )
        macroCommand.execute(context)
        macroCommand.undo(context)
        assertRekkefølge("B før", "C før", "C etter", "B etter")
        assertTellere(2, 0, 2)
    }

    @Test
    fun `Undo reverserer alle kjørte kommandoer når en kommando suspender`() {
        val macroCommand =
            command(
                execute = { constants.add("B før"); false },
                undo = { constants.add("B etter") }
            ) +
            command(
                execute = { constants.add("C før"); true },
                undo = { constants.add("C etter") }
            )
        macroCommand.execute(context)
        macroCommand.undo(context)
        assertRekkefølge("B før", "B etter")
        assertTellere(1, 0, 1)
    }

    @Test
    fun `Undo reverserer riktig etter restore`() {
        val macroCommand =
            command(
                execute = { constants.add("B før"); false },
                undo = { constants.add("B etter") }
            ) +
            command(
                execute = { constants.add("C før"); true },
                undo = { constants.add("C etter") }
            )
        macroCommand.undo(CommandContext(UUID.randomUUID(), listOf(1)))
        assertRekkefølge("C etter", "B etter")
        assertTellere(0, 0, 2)
    }

    @Test
    fun `Undo reverserer riktig etter resume`() {
        val macroCommand =
            command(
                execute = { constants.add("B før"); true },
                undo = { constants.add("B undo") }
            ) +
            command(
                execute = { constants.add("C før"); false },
                resume = { constants.add("C etter"); true },
                undo = { constants.add("C undo") }
            )
        context = CommandContext(UUID.randomUUID(), listOf(1))
        macroCommand.resume(context)
        macroCommand.undo(context)
        assertRekkefølge("C etter", "C undo", "B undo")
        assertTellere(0, 1, 2)
    }

    private fun assertRekkefølge(vararg konstanter: String) {
        assertEquals(konstanter.toList(), constants)
    }

    private fun assertTellere(expectedExecuteCount: Int, expectedResumeCount: Int, expectedUndoCount: Int) {
        assertEquals(expectedExecuteCount, executeCount)
        assertEquals(expectedResumeCount, resumeCount)
        assertEquals(expectedUndoCount, undoCount)
    }

    private operator fun Command.plus(other: Command): MacroCommand {
        return object : MacroCommand() {
            override val commands: List<Command> = listOf(this@plus, other)
        }
    }

    private fun command(execute: (context: CommandContext) -> Boolean, resume: (context: CommandContext) -> Boolean = { true }, undo: () -> Unit = {}): Command {
        return object : Command {
            override fun execute(context: CommandContext): Boolean {
                executeCount += 1
                return execute(context)
            }

            override fun resume(context: CommandContext): Boolean {
                resumeCount += 1
                return resume(context)
            }

            override fun undo(context: CommandContext) {
                undoCount += 1
                return undo()
            }
        }
    }
}
