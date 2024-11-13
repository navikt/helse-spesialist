package no.nav.helse

import no.nav.helse.modell.behov.Behov
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal fun Collection<Behov>.somJsonMessage(
    contextId: UUID,
    fødselsnummer: String,
    hendelseId: UUID,
): JsonMessage {
    return JsonMessage.newNeed(
        behov = this.map { behov -> behov.behovName() },
        map =
            this.associate {
                it.behovName() to it.somJsonMessage()
            } +
                mapOf(
                    "fødselsnummer" to fødselsnummer,
                    "contextId" to contextId,
                    "hendelseId" to hendelseId,
                ),
    )
}
