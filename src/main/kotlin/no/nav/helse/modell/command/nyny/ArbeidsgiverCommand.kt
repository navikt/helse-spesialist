package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao

internal class ArbeidsgiverCommand(organisasjonsnummer: String, arbeidsgiverDao: ArbeidsgiverDao) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao),
        OppdaterArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao)
    )
}
