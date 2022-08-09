package no.nav.helse.modell.arbeidsforhold.command

import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand

internal class KlargjørArbeidsforholdCommand(
    fødselsnummer: String,
    organisasjonsnummer: String,
    arbeidsforholdDao: ArbeidsforholdDao,
    førstegangsbehandling: Boolean
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            organisasjonsnummer = organisasjonsnummer
        ),
        OppdaterArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            førstegangsbehandling = førstegangsbehandling
        )
    )
}
