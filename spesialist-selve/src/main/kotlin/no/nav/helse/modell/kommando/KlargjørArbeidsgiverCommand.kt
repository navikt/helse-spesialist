package no.nav.helse.modell.kommando

import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao

internal class KlargjørArbeidsgiverCommand(
    orgnummere: List<String>,
    arbeidsgiverDao: ArbeidsgiverDao,
) : MacroCommand() {
    private val arbeidsgivere = orgnummere.distinct()
    override val commands: List<Command> =
        listOf(
            OpprettArbeidsgiverCommand(arbeidsgivere, arbeidsgiverDao),
            OppdaterArbeidsgiverCommand(arbeidsgivere, arbeidsgiverDao),
        )
}
