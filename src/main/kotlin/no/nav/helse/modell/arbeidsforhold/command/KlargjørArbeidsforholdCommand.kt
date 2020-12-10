package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand

internal class KlargjørArbeidsforholdCommand(
    fødselsnummer: String,
    organisasjonsnummer: String,
    arbeidsforholdDao: ArbeidsforholdDao,
    miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            organisasjonsnummer = organisasjonsnummer,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
        ),
        OppdaterArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
        )
    )
}
