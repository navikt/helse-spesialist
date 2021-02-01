package no.nav.helse.modell.abonnement

import java.util.*

data class OpptegnelseDto(
    val aktørId: Long,
    val sekvensnummer: Int,
    val type: OpptegnelseType,
    val payload: String
)

enum class OpptegnelseType {
    UTBETALING_ANNULLERING_FEILET,
    UTBETALING_ANNULLERING_OK,
    FERDIGBEHANDLET_GODKJENNIGSBEHOV
}

sealed class PayloadToSpeil {
    abstract fun toJson(): String
}

data class UtbetalingPayload(private val utbetalingId: UUID): PayloadToSpeil() {
    override fun toJson() = """
        { "utbetalingId": "$utbetalingId" }
    """.trimIndent()
}

data class GodkjenningsbehovPayload(private val hendelseId: UUID): PayloadToSpeil() {
    override fun toJson() = """
        { "hendelseId": "$hendelseId" }
    """.trimIndent()
}
