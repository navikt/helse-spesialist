package no.nav.helse.modell.command.nyny

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class MacroCommandTest {
    private val testConstants: MutableList<String> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
        testConstants.clear()
    }

    @Test
    fun `Kommandoer utføres i rekkefølge`() {
        val command1 = command { testConstants.add("Kommando A"); true }
        val command2 = command { testConstants.add("Kommando B"); true }
        val macroCommand = command1 + command2
        assertTrue(macroCommand.execute())
        assertRekkefølge("Kommando A", "Kommando B")
    }

    @Test
    fun `Kommandoer kan suspenderes`() {
        val command1 = command { testConstants.add("Kommando A"); false }
        val command2 = command { testConstants.add("Kommando B"); true }
        val macroCommand = command1 + command2
        assertFalse(macroCommand.execute())
        assertRekkefølge("Kommando A")
    }

    @Test
    fun `Kommandoer kan fortsette`() {
        var Ok = false
        val command1 = command { testConstants.add("Kommando A ${if(!Ok) "Før" else "Etter"}"); Ok }
        val command2 = command { testConstants.add("Kommando B"); true }
        val macroCommand = command1 + command2
        macroCommand.execute()
        Ok = true
        assertTrue(macroCommand.execute())
        assertRekkefølge("Kommando A Før", "Kommando A Etter", "Kommando B")
    }

    private fun assertRekkefølge(vararg konstanter: String) {
        assertEquals(konstanter.toList(), testConstants)
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
