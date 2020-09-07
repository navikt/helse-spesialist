package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.person.PersonDao

internal class KlargjørPersonCommand(fødselsnummer: String, aktørId: String, personDao: PersonDao) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettPersonCommand(fødselsnummer, aktørId, personDao),
        OppdaterPersonCommand(fødselsnummer, personDao)
    )
}
