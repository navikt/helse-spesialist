package no.nav.helse.modell.automatisering

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import org.slf4j.LoggerFactory
import java.util.UUID

internal class SettTidligereAutomatiseringInaktivCommand(
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(SettTidligereAutomatiseringInaktivCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        logg.info("Setter rader inaktive i automatisering og automatisering_problem for vedtaksperiode $vedtaksperiodeId")
        automatisering.settInaktiv(vedtaksperiodeId, hendelseId)
        return true
    }
}
