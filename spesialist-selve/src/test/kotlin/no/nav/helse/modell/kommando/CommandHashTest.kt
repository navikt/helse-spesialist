package no.nav.helse.modell.kommando

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CommandHashTest {

    @Test
    fun `To kommandoer med samme navn har samme hash`() {
        val første = commandA()
        val andre = commandA()

        assertEquals(første.hash(), andre.hash())
    }

    @Test
    fun `To kommandoer med forskjellig navn har forskjellig hash`() {
        val første = commandA()
        val andre = commandB()

        assertNotEquals(første.hash(), andre.hash())
    }

    @Test
    fun `To makrokommandoer med samme navn har samme hash`() {
        val første = macroA()
        val andre = macroA()
        assertEquals(første.hash(), andre.hash())
    }

    @Test
    fun `To makrokommandoer med forskjellig navn har forskjellig hash`() {
        val første = macroA()
        val andre = macroB()
        assertNotEquals(første.hash(), andre.hash())
    }

    @Test
    fun `En makrokommando har samme hash hvis kjeden er lik`() {
        val første = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandA(), commandB())
        }
        val andre = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandA(), commandB())
        }

        assertEquals(første.hash(), andre.hash())
    }

    @Test
    fun `En anonym makrokommando har lik hash som en annen anonym makro dersom kjeden ellers er lik`() {
        val første = object: MacroCommand() {
            override val commands: List<Command> = listOf()
        }
        val andre = object: MacroCommand() {
            override val commands: List<Command> = listOf()
        }

        assertEquals(første.hash(), andre.hash())
    }

    @Test
    fun `En makrokommando har forskjellig hash hvis kommandoer har byttet rekkefølge`() {
        val første = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandA(), commandB())
        }
        val andre = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandB(), commandA())
        }

        assertNotEquals(første.hash(), andre.hash())
    }

    @Test
    fun `En makrokommando har forskjellig hash hvis kommandoer har blitt fjernet`() {
        val første = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandA(), commandB())
        }
        val andre = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandB())
        }

        assertNotEquals(første.hash(), andre.hash())
    }

    @Test
    fun `En makrokommando har forskjellig hash hvis kommandoer har blitt lagt til`() {
        val første = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandA(), commandB())
        }
        val andre = object: MacroCommand() {
            override val commands: List<Command> = listOf(commandA(), commandB(), commandC())
        }

        assertNotEquals(første.hash(), andre.hash())
    }

    private class macroA : MacroCommand() {
        override val commands: List<Command> = emptyList()
    }

    private class macroB : MacroCommand() {
        override val commands: List<Command> = emptyList()
    }

    private class commandA : Command {
        override fun execute(context: CommandContext): Boolean = true
    }

    private class commandB: Command {
        override fun execute(context: CommandContext): Boolean = true
    }

    private class commandC: Command {
        override fun execute(context: CommandContext): Boolean = true
    }
}