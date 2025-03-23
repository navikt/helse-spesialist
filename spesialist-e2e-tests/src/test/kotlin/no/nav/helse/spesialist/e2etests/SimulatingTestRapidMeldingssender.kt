package no.nav.helse.spesialist.e2etests

import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

class SimulatingTestRapidMeldingssender(private val rapid: SimulatingTestRapid) {
    private val newUUID get() = UUID.randomUUID()

    fun sendEgenAnsattløsning(
        aktørId: String,
        fødselsnummer: String,
        erEgenAnsatt: Boolean,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("EgenAnsatt", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagEgenAnsattløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                erEgenAnsatt = erEgenAnsatt,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }

    fun sendVergemålOgFullmaktløsning(
        aktørId: String,
        fødselsnummer: String,
        vergemål: List<Vergemål> = emptyList(),
        fremtidsfullmakter: List<Vergemål> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals(listOf("Vergemål", "Fullmakt"), behov["@behov"].map { it.asText() })
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        val payload = Testmeldingfabrikk.VergemålJson(vergemål, fremtidsfullmakter, fullmakter)

        rapid.publish(
            Testmeldingfabrikk.lagVergemålOgFullmaktKomposittLøsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vergemål = payload,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

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

    fun sendÅpneGosysOppgaverløsning(
        aktørId: String,
        fødselsnummer: String,
        antall: Int,
        oppslagFeilet: Boolean,
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("ÅpneOppgaver", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagÅpneGosysOppgaverløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendRisikovurderingløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        funn: List<Risikofunn> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = rapid.messageLog.filter { it.path("@event_name").asText() == "behov" }.last()
        assertEquals("Risikovurdering", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        rapid.publish(
            Testmeldingfabrikk.lagRisikovurderingløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                funn = funn,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }
}
