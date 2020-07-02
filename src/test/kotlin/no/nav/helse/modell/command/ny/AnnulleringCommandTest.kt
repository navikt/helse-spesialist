package no.nav.helse.modell.command.ny

import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.meldinger.AnnulleringMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AnnulleringCommandTest {
    @Test
    fun `annullering-command legger annulleringsmelding på rapid`() {
        val dataSource = setupDataSourceMedFlyway()
        val testRapid = TestRapid()
        val annulleringMessage = AnnulleringMessage(
            aktørId = "aktørid",
            fødselsnummer = "fødselsnummer",
            organisasjonsnummer = "organisasjonsnummer",
            fagsystemId = "fagsystemid",
            saksbehandler = "saksbehandler"
        )
        val annulleringCommand = AnnulleringCommand(testRapid, annulleringMessage)
        sessionOf(dataSource, returnGeneratedKey = true).use(annulleringCommand::execute)

        val kanselleringsmelding = testRapid.inspektør.message(0)

        assertEquals("kanseller_utbetaling", kanselleringsmelding["@event_name"].asText())
        assertEquals(annulleringMessage.fagsystemId, kanselleringsmelding["fagsystemId"].asText())
    }
}
