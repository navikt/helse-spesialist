package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import java.util.*

internal class OpprettSaksbehandlerCommand(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val saksbehandlerDao: SaksbehandlerDao
) : Command {
    override fun execute(context: CommandContext): Boolean {
        saksbehandlerDao.opprettSaksbehandler(oid, navn, epost)

        return true
    }
}
