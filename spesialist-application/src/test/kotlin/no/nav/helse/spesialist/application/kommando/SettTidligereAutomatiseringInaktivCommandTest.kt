package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SettTidligereAutomatiseringInaktivCommandTest : ApplicationTest() {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val hendelseId = UUID.randomUUID()
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val command =
        SettTidligereAutomatiseringInaktivCommand(
            vedtaksperiodeId,
            hendelseId,
            automatisering,
        )

    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())

    @Test
    fun `kaller utfør og returnerer true`() {
        assertTrue(command.execute(commandContext, sessionContext, outbox))

        verify {
            automatisering.settInaktiv(vedtaksperiodeId, hendelseId)
        }
    }
}
