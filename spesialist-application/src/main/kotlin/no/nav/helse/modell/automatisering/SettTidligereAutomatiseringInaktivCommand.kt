package no.nav.helse.modell.automatisering

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import java.util.UUID

internal class SettTidligereAutomatiseringInaktivCommand(
    private val vedtaksperiodeId: UUID,
    private val hendelseId: UUID,
    private val automatisering: Automatisering,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        logg.info("Setter rader inaktive i automatisering og automatisering_problem for vedtaksperiode $vedtaksperiodeId")
        automatisering.settInaktiv(vedtaksperiodeId, hendelseId)
        return true
    }
}
