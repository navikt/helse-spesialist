package no.nav.helse.modell.opptegnelse

import no.nav.helse.abonnement.OpptegnelseType
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import java.util.*

internal class OpprettOpptegnelseCommand(
    private val opptegnelseDao: OpptegnelseDao,
    private val fødselsnummer: String,
    private val hendelseId: UUID,
    private val opptegnelseType: OpptegnelseType
): Command {
    override fun execute(context: CommandContext): Boolean {
        opptegnelseDao.opprettOpptegnelse(
            fødselsnummer,
            GodkjenningsbehovPayload(hendelseId),
            opptegnelseType
        )
        return true
    }
}
