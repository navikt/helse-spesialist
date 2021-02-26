package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand

internal class KlargjørArbeidsforholdCommand(
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    arbeidsforholdDao: ArbeidsforholdDao
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsforholdCommand(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            organisasjonsnummer = organisasjonsnummer
        ),
        OppdaterArbeidsforholdCommand(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao
        )
    )
}
