package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand

internal class KlargjørArbeidsforholdCommand(
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    arbeidsforholdDao: ArbeidsforholdDao,
    miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsforholdCommand(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            organisasjonsnummer = organisasjonsnummer,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
        ),
        OppdaterArbeidsforholdCommand(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
        )
    )
}
