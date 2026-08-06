package no.nav.helse.spesialist.api

import tools.jackson.databind.DatabindException

internal fun DatabindException.erKlientFrakoblingUnderSerialisering(): Boolean {
    val melding = message ?: return false
    return melding.contains("Broken pipe", ignoreCase = true) ||
        melding.contains("Connection reset by peer", ignoreCase = true)
}
