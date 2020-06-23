package no.nav.helse.modell.command

import java.time.Duration
import java.util.*

abstract class MacroCommand(
    eventId: UUID,
    timeout: Duration
) : Command(
    eventId = eventId,
    parent = null,
    timeout = timeout
) {
    internal abstract val fødselsnummer: String
    internal abstract val orgnummer: String?
    internal abstract val vedtaksperiodeId: UUID?

    internal abstract fun toJson(): String
}
