package no.nav.helse.modell.kommando

import no.nav.helse.modell.person.PersonDao
import java.util.*

internal class KlargjørPersonCommand(
    fødselsnummer: String,
    aktørId: String,
    personDao: PersonDao,
    godkjenningsbehovJson: String,
    vedtaksperiodeId: UUID
) : MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettPersonCommand(fødselsnummer, aktørId, personDao, godkjenningsbehovJson, vedtaksperiodeId),
        OppdaterPersonCommand(fødselsnummer, personDao, godkjenningsbehovJson, vedtaksperiodeId)
    )
}
