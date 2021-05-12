package no.nav.helse.modell.opptegnelse

import java.util.*

sealed class OpptegnelsePayload {
    abstract fun toJson(): String
}

internal data class UtbetalingPayload(private val utbetalingId: UUID) : OpptegnelsePayload() {
    override fun toJson() = """
        { "utbetalingId": "$utbetalingId" }
    """.trimIndent()
}

internal data class GodkjenningsbehovPayload(private val hendelseId: UUID) : OpptegnelsePayload() {
    override fun toJson() = """
        { "hendelseId": "$hendelseId" }
    """.trimIndent()
}

