package no.nav.helse.modell.kommando

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao

internal class KlargjørArbeidsgiverCommand(
    organisasjonsnummer: String,
    arbeidsgiverDao: ArbeidsgiverDao,
    miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao, miljøstyrtFeatureToggle),
        OppdaterArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao, miljøstyrtFeatureToggle)
    )
}
