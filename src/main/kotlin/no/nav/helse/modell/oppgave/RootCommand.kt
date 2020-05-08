package no.nav.helse.modell.oppgave

import java.time.Duration
import java.util.*

abstract class RootCommand(
    behovId: UUID,
    timeout: Duration
) : Command(
    behovId = behovId,
    parent = null,
    timeout = timeout
) {
    internal abstract val fødselsnummer: String
    internal abstract val orgnummer: String?
    internal abstract val vedtaksperiodeId: UUID?

    internal abstract fun toJson(): String
}
