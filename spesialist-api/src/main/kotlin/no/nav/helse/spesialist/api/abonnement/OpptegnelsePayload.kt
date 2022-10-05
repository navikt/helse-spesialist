package no.nav.helse.spesialist.api.abonnement

import java.time.LocalDateTime
import java.util.*
import org.intellij.lang.annotations.Language

sealed class OpptegnelsePayload {
    abstract fun toJson(): String
}

data class UtbetalingPayload(private val utbetalingId: UUID) : OpptegnelsePayload() {
    override fun toJson() = """
        { "utbetalingId": "$utbetalingId" }
    """.trimIndent()
}

data class GodkjenningsbehovPayload(private val hendelseId: UUID) : OpptegnelsePayload() {
    override fun toJson() = """
        { "hendelseId": "$hendelseId" }
    """.trimIndent()
    companion object {
        fun GodkjenningsbehovPayload.lagre(opptegnelseDao: OpptegnelseDao, fødselsnummer: String) {
            opptegnelseDao.opprettOpptegnelse(
                fødselsnummer = fødselsnummer,
                payload = this,
                // Dette er et litt misvisende navn, opprettes både når oppgave opprettes, men også når noe avvises/betales automatisk
                type = OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE
            )
        }
    }
}

data class RevurderingAvvistPayload(private val hendelseId: UUID, private val errors: List<String>) :
    OpptegnelsePayload() {
    override fun toJson() = """
        { "hendelseId": "$hendelseId", "errors": ${errors.map{ "\"$it\""}} }
    """.trimIndent()
}

object PersonOppdatertPayload : OpptegnelsePayload() {
    @Language("json")
    override fun toJson() = """
        { "oppdatert": "${LocalDateTime.now()}" }
    """.trimIndent()
}
