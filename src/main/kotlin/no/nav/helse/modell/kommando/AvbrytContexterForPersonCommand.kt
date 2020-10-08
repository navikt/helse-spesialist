package no.nav.helse.modell.kommando

import no.nav.helse.modell.CommandContextDao

internal class AvbrytContexterForPersonCommand(
    private val fødselsnummer: String,
    private val commandContextDao: CommandContextDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        context.avbryt(commandContextDao, fødselsnummer)
        return true
    }
}
