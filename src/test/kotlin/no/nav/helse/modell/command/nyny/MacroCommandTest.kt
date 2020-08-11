package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class MacroCommandTest {
    private val constants: MutableList<String> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
        constants.clear()
    }

    @Test
    fun `Kommandoer utføres i rekkefølge`() {
        val command1 = command { constants.add("Kommando A"); true }
        val command2 = command { constants.add("Kommando B"); true }
        val macroCommand = command1 + command2
        assertTrue(macroCommand.execute())
        assertRekkefølge("Kommando A", "Kommando B")
        assertTrue(macroCommand.state().isEmpty())
    }

    @Test
    fun `Kommandoer kan suspenderes`() {
        val command1 = command { constants.add("Kommando A"); false }
        val command2 = command { constants.add("Kommando B"); true }
        val macroCommand = command1 + command2
        assertFalse(macroCommand.execute())
        assertRekkefølge("Kommando A")
        assertEquals(listOf(0), macroCommand.state())
    }

    @Test
    fun `Kommandoer kan fortsette`() {
        var Ok = false
        val command1 = command { constants.add("Kommando A ${if(!Ok) "Før" else "Etter"}"); Ok }
        val command2 = command { constants.add("Kommando B"); true }
        val macroCommand = command1 + command2
        macroCommand.execute()
        Ok = true
        assertTrue(macroCommand.execute())
        assertRekkefølge("Kommando A Før", "Kommando A Etter", "Kommando B")
        assertTrue(macroCommand.state().isEmpty())
    }

    @Test
    fun `Nestede makrokommandoer`() {
        var ok = false
        val command1 = command { constants.add("A"); true }
        val macroCommand1 = command { constants.add("B"); ok } + command { constants.add("C"); !ok }
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.execute()
        assertEquals(listOf(1, 0), macroCommand2.state())
        ok = true
        macroCommand2.execute()
        assertEquals(listOf(1, 1), macroCommand2.state())
        ok = false
        macroCommand2.execute()
        assertTrue(macroCommand2.state().isEmpty())
        assertRekkefølge("A", "B", "B", "C", "C")
    }

    private fun assertRekkefølge(vararg konstanter: String) {
        assertEquals(konstanter.toList(), constants)
    }

    private operator fun Command.plus(other: Command): MacroCommand {
        return object : MacroCommand() {
            override val commands: List<Command> = listOf(this@plus, other)
        }
    }

    private fun command(f: () -> Boolean): Command {
        return object : Command() {
            override fun execute(): Boolean {
                return f()
            }
        }
    }
}
