package no.nav.helse.spesialist.api.abonnement

import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

sealed class OpptegnelsePayload {
    abstract fun toJson(): String
}

data class UtbetalingPayload(private val utbetalingId: UUID) : OpptegnelsePayload() {
    override fun toJson() =
        """
        { "utbetalingId": "$utbetalingId" }
        """.trimIndent()
}

data class GodkjenningsbehovPayload(private val hendelseId: UUID) : OpptegnelsePayload() {
    override fun toJson() =
        """
        { "hendelseId": "$hendelseId" }
        """.trimIndent()
}

enum class AutomatiskBehandlingUtfall {
    UTBETALT,
    AVVIST,
}

data class AutomatiskBehandlingPayload(private val hendelseId: UUID, private val utfall: AutomatiskBehandlingUtfall) : OpptegnelsePayload() {
    override fun toJson() =
        """
        { "hendelseId": "$hendelseId", "utfall": "$utfall" }
        """.trimIndent()
}

data object PersonOppdatertPayload : OpptegnelsePayload() {
    @Language("json")
    override fun toJson() =
        """
        { "oppdatert": "${LocalDateTime.now()}" }
        """.trimIndent()
}
