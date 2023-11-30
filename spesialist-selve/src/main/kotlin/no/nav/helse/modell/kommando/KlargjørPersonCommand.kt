package no.nav.helse.modell.kommando

import java.time.LocalDate
import no.nav.helse.modell.person.PersonDao

internal class KlargjørPersonCommand(
    fødselsnummer: String,
    aktørId: String,
    førsteKjenteDagFinner: () -> LocalDate,
    personDao: PersonDao,
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettPersonCommand(fødselsnummer, aktørId, førsteKjenteDagFinner, personDao),
        OppdaterPersonCommand(fødselsnummer, førsteKjenteDagFinner, personDao),
    )
}
