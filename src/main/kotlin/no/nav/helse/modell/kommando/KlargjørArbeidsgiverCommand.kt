package no.nav.helse.modell.kommando

import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao

internal class KlargjørArbeidsgiverCommand(organisasjonsnummer: String, arbeidsgiverDao: ArbeidsgiverDao) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao),
        OppdaterArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao)
    )
}
