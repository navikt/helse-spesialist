package no.nav.helse.modell.kommando

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao

internal class KlargjørArbeidsgiverCommand(
    orgnummere: List<String>,
    arbeidsgiverDao: ArbeidsgiverDao,
    miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsgiverCommand(orgnummere, arbeidsgiverDao, miljøstyrtFeatureToggle),
        OppdaterArbeidsgiverCommand(orgnummere, arbeidsgiverDao, miljøstyrtFeatureToggle)
    )
}
