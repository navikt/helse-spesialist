package no.nav.helse.modell.kommando

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.person.PersonDao
import java.util.*

internal class KlargjørPersonCommand(
    fødselsnummer: String,
    aktørId: String,
    personDao: PersonDao,
    godkjenningsbehovJson: String,
    vedtaksperiodeId: UUID,
    godkjenningMediator: GodkjenningMediator,
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettPersonCommand(fødselsnummer, aktørId, personDao, godkjenningsbehovJson, vedtaksperiodeId, godkjenningMediator),
        OppdaterPersonCommand(fødselsnummer, personDao, godkjenningsbehovJson, vedtaksperiodeId, godkjenningMediator)
    )
}
