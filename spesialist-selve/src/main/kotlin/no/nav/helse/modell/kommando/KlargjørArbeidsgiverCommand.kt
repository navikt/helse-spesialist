package no.nav.helse.modell.kommando

import no.nav.helse.arbeidsgiver.ArbeidsgiverDao

internal class KlargjørArbeidsgiverCommand(
    orgnummere: List<String>,
    arbeidsgiverDao: ArbeidsgiverDao
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsgiverCommand(orgnummere, arbeidsgiverDao),
        OppdaterArbeidsgiverCommand(orgnummere, arbeidsgiverDao)
    )
}
