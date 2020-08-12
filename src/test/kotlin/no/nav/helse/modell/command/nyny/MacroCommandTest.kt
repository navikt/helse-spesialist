package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MacroCommandTest {
    private val constants: MutableList<String> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
        constants.clear()
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

    private fun assertRekkefølge(vararg konstanter: String) {
        assertEquals(konstanter.toList(), constants)
    }

    private operator fun Command.plus(other: Command): MacroCommand {
        return object : MacroCommand() {
            override val commands: List<Command> = listOf(this@plus, other)
        }
    }

    private fun command(execute: () -> Boolean, resume: () -> Boolean = { true }): Command {
        return object : Command() {
            override fun execute(): Boolean {
                return execute()
            }

            override fun resume(): Boolean {
                return resume()
            }
        }
    }
}
