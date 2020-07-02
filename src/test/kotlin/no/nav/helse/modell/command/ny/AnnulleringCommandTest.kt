package no.nav.helse.modell.command.ny

import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.meldinger.AnnulleringMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AnnulleringCommandTest {
    @Test
    fun sd() {
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
        annulleringCommand.execute(sessionOf(dataSource, returnGeneratedKey = true))

        val kanselleringsmelding = testRapid.inspektør.message(0)

        assertEquals("kanseller_utbetaling", kanselleringsmelding["@event_name"].asText())
        assertEquals(annulleringMessage.fagsystemId, kanselleringsmelding["fagsystemId"].asText())
    }
}
