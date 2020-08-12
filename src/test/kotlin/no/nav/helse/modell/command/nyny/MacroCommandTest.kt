package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MacroCommandTest {
    private val constants: MutableList<String> = mutableListOf()
    private var executeCount: Int = 0
    private var resumeCount: Int = 0
    private var undoCount: Int = 0

    @BeforeEach
    fun beforeEach() {
        constants.clear()
        executeCount = 0
        resumeCount = 0
        undoCount = 0
    }

    @Test
    fun `Kommandoer utføres i rekkefølge`() {
        val command1 = command(execute = { constants.add("Kommando A"); true })
        val command2 = command(execute = { constants.add("Kommando B"); true })
        val macroCommand = command1 + command2
        assertTrue(macroCommand.execute())
        assertRekkefølge("Kommando A", "Kommando B")
        assertTrue(macroCommand.state().isEmpty())
    }

    @Test
    fun `Kommandoer kan suspenderes`() {
        val command1 = command(execute = { constants.add("Kommando A"); false })
        val command2 = command(execute = { constants.add("Kommando B"); true })
        val macroCommand = command1 + command2
        assertFalse(macroCommand.execute())
        assertRekkefølge("Kommando A")
        assertEquals(listOf(0), macroCommand.state())
    }

    @Test
    fun `Kommandoer kan fortsette`() {
        val command1 = command(
            execute = { constants.add("Kommando A Før"); false },
            resume = { constants.add("Kommando A Etter"); true }
        )
        val command2 = command(execute = { constants.add("Kommando B"); true })
        val macroCommand = command1 + command2
        macroCommand.execute()
        assertTrue(macroCommand.resume())
        assertRekkefølge("Kommando A Før", "Kommando A Etter", "Kommando B")
        assertTrue(macroCommand.state().isEmpty())
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
        macroCommand2.execute()
        assertEquals(listOf(1, 0), macroCommand2.state())
        macroCommand2.resume()
        assertEquals(listOf(1, 1), macroCommand2.state())
        macroCommand2.resume()
        assertTrue(macroCommand2.state().isEmpty())
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
        macroCommand2.restore(listOf(1, 0))
        macroCommand2.resume()
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
        macroCommand2.restore(listOf(1, 0))
        macroCommand2.execute()
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
        macroCommand.undo()
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
        macroCommand.execute()
        macroCommand.undo()
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
        macroCommand.execute()
        macroCommand.undo()
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
        macroCommand.restore(listOf(1))
        macroCommand.undo()
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
        macroCommand.restore(listOf(1))
        macroCommand.resume()
        macroCommand.undo()
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

    private fun command(execute: () -> Boolean, resume: () -> Boolean = { true }, undo: () -> Unit = {}): Command {
        return object : Command() {
            override fun execute(): Boolean {
                executeCount += 1
                return execute()
            }

            override fun resume(): Boolean {
                resumeCount += 1
                return resume()
            }

            override fun undo() {
                undoCount += 1
                return undo()
            }
        }
    }
}
