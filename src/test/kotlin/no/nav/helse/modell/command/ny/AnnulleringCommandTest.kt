package no.nav.helse.modell.command.ny

import AbstractEndToEndTest
import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.meldinger.AnnulleringMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AnnulleringCommandTest : AbstractEndToEndTest() {
    @Test
    fun `annullering-command legger annulleringsmelding på rapid`() {
        val annulleringMessage = AnnulleringMessage(
            aktørId = "aktørid",
            fødselsnummer = "fødselsnummer",
            organisasjonsnummer = "organisasjonsnummer",
            fagsystemId = "fagsystemid",
            saksbehandler = "saksbehandler",
            saksbehandlerEpost = "saksbehander@nav.no"
        )
        val annulleringCommand = AnnulleringCommand(testRapid, annulleringMessage)
        sessionOf(dataSource, returnGeneratedKey = true).use(annulleringCommand::execute)

        val kanselleringsmelding = testRapid.inspektør.message(0)

        assertEquals("kanseller_utbetaling", kanselleringsmelding["@event_name"].asText())
        assertEquals(annulleringMessage.fagsystemId, kanselleringsmelding["fagsystemId"].asText())
    }
}
