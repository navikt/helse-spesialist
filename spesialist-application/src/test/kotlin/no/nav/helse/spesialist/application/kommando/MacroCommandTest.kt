package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.spesialist.application.Outbox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID

internal class MacroCommandTest : ApplicationTest() {
    private val constants: MutableList<String> = mutableListOf()
    private var executeCount: Int = 0
    private var resumeCount: Int = 0

    private lateinit var commandContext: CommandContext

    @BeforeEach
    fun beforeEach() {
        constants.clear()
        executeCount = 0
        resumeCount = 0
        commandContext = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Kommandoer utføres i rekkefølge`() {
        val command1 =
            command(execute = {
                constants.add("Kommando A")
                true
            })
        val command2 =
            command(execute = {
                constants.add("Kommando B")
                true
            })
        val macroCommand = command1 + command2
        assertTrue(macroCommand.execute(commandContext, sessionContext, outbox))
        assertRekkefølge("Kommando A", "Kommando B")
        assertTrue(commandContext.sti().isEmpty())
    }

    @Test
    fun `Kommandoer kan suspenderes`() {
        val command1 =
            command(execute = {
                constants.add("Kommando A")
                false
            })
        val command2 =
            command(execute = {
                constants.add("Kommando B")
                true
            })
        val macroCommand = command1 + command2
        assertFalse(macroCommand.execute(commandContext, sessionContext, outbox))
        assertRekkefølge("Kommando A")
        assertEquals(listOf(0), commandContext.sti())
    }

    @Test
    fun `Kommandoer kan fortsette`() {
        val command1 =
            command(
                execute = {
                    constants.add("Kommando A Før")
                    false
                },
                resume = {
                    constants.add("Kommando A Etter")
                    true
                },
            )
        val command2 =
            command(execute = {
                constants.add("Kommando B")
                true
            })
        val macroCommand = command1 + command2
        macroCommand.execute(commandContext, sessionContext, outbox)
        assertTrue(macroCommand.resume(commandContext, sessionContext, outbox))
        assertRekkefølge("Kommando A Før", "Kommando A Etter", "Kommando B")
        assertTrue(commandContext.sti().isEmpty())
    }

    @Test
    fun `Nestede makrokommandoer`() {
        val command1 =
            command(execute = {
                constants.add("A")
                true
            })
        val macroCommand1 =
            command(
                execute = {
                    constants.add("B før")
                    false
                },
                resume = {
                    constants.add("B etter")
                    true
                },
            ) +
                command(
                    execute = {
                        constants.add("C før")
                        false
                    },
                    resume = {
                        constants.add("C etter")
                        true
                    },
                )
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.execute(commandContext, sessionContext, outbox)
        assertEquals(listOf(1, 0), commandContext.sti())
        macroCommand2.resume(commandContext, sessionContext, outbox)
        assertEquals(listOf(1, 1), commandContext.sti())
        macroCommand2.resume(commandContext, sessionContext, outbox)
        assertTrue(commandContext.sti().isEmpty())
        assertRekkefølge("A", "B før", "B etter", "C før", "C etter")
    }

    @Test
    fun `Restore av kommandohierkarki`() {
        val command1 =
            command(execute = {
                constants.add("A")
                true
            })
        val macroCommand1 =
            command(
                execute = {
                    constants.add("B før")
                    false
                },
                resume = {
                    constants.add("B etter")
                    true
                },
            ) +
                command(
                    execute = {
                        constants.add("C før")
                        false
                    },
                    resume = {
                        constants.add("C etter")
                        true
                    },
                )
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.resume(CommandContext(UUID.randomUUID(), listOf(1, 0)), sessionContext, outbox)
        assertRekkefølge("B etter", "C før")
    }

    @Test
    fun `Execute etter restore medfører at execution starter fra begynnelsen`() {
        val command1 =
            command(execute = {
                constants.add("A")
                true
            })
        val macroCommand1 =
            command(
                execute = {
                    constants.add("B før")
                    false
                },
                resume = {
                    constants.add("B etter")
                    true
                },
            ) +
                command(
                    execute = {
                        constants.add("C før")
                        false
                    },
                    resume = {
                        constants.add("C etter")
                        true
                    },
                )
        val macroCommand2 = command1 + macroCommand1
        macroCommand2.execute(CommandContext(UUID.randomUUID(), listOf(1, 0)), sessionContext, outbox)
        assertRekkefølge("A", "B før")
    }

    @Test
    fun `Utfører ikke commands etter at context er ferdigstilt i execute`() {
        val macroCommand =
            command(
                execute = { this.ferdigstill(commandContext) },
            ) +
                command(
                    execute = { throw Exception() },
                )
        assertDoesNotThrow { macroCommand.execute(commandContext, sessionContext, outbox) }
    }

    @Test
    fun `Utfører ikke commands etter at context er ferdigstilt i resume`() {
        val macroCommand =
            command(
                execute = { throw Exception() },
                resume = { this.ferdigstill(commandContext) },
            ) +
                command(
                    execute = { throw Exception() },
                )
        assertDoesNotThrow { macroCommand.resume(commandContext, sessionContext, outbox) }
    }

    private fun assertRekkefølge(vararg konstanter: String) {
        assertEquals(konstanter.toList(), constants)
    }

    private operator fun Command.plus(other: Command): MacroCommand =
        object : MacroCommand() {
            override val commands: List<Command> = listOf(this@plus, other)
        }

    private fun command(
        execute: Command.(commandContext: CommandContext) -> Boolean,
        resume: Command.(commandContext: CommandContext) -> Boolean = { true },
    ): Command {
        return object : Command {
            override fun execute(
                commandContext: CommandContext,
                sessionContext: SessionContext,
                outbox: Outbox,
            ): Boolean {
                executeCount += 1
                return execute(this, this@MacroCommandTest.commandContext)
            }

            override fun resume(
                commandContext: CommandContext,
                sessionContext: SessionContext,
                outbox: Outbox,
            ): Boolean {
                resumeCount += 1
                return resume(this, this@MacroCommandTest.commandContext)
            }
        }
    }
}
