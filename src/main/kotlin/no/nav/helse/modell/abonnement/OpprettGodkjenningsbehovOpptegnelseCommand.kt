package no.nav.helse.modell.abonnement

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import java.util.*

internal class OpprettGodkjenningsbehovOpptegnelseCommand(
    private val opptegnelseDao: OpptegnelseDao,
    private val fødselsnummer: String,
    private val hendelseId: UUID
): Command {
    override fun execute(context: CommandContext): Boolean {
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer,
            GodkjenningsbehovPayload(hendelseId),
            OpptegnelseType.FERDIGBEHANDLET_GODKJENNIGSBEHOV
        )
        return true
    }
}
