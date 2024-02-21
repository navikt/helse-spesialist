package no.nav.helse.modell.person

import java.util.UUID
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettMinimalArbeidsgiverCommand
import no.nav.helse.modell.kommando.OpprettMinimalPersonCommand

internal class SøknadSendt(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    private val json: String,
) : Personhendelse {
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}

internal class SøknadSendtCommand(
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettMinimalPersonCommand(fødselsnummer, aktørId, personDao),
        OpprettMinimalArbeidsgiverCommand(organisasjonsnummer, arbeidsgiverDao),
    )
}