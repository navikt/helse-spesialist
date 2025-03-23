package no.nav.helse.spesialist.e2etests

import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

class SimulatingTestRapidMeldingssender(private val rapid: SimulatingTestRapid) {
    private val newUUID get() = UUID.randomUUID()

    fun sendInntektløsning(
        aktørId: String,
        fødselsnummer: String,
        orgnr: String,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("InntekterForSykepengegrunnlag", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagInntektløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                orgnummer = orgnr,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }
}
