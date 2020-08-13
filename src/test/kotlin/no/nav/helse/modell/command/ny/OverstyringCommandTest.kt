package no.nav.helse.modell.command.ny

import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OverstyringCommandTest {
    @Test
    fun `overstyring-command legger ovserstyringsmelding på rapid`() {
        val dataSource = setupDataSourceMedFlyway()

        val testRapid = TestRapid()
        val overstyringMessage = OverstyringMessage(
            aktørId = "aktørid",
            fødselsnummer = "12345678910",
            organisasjonsnummer = "987654321",
            begrunnelse = "En god grunn",
            unntaFraInnsyn = false,
            dager = objectMapper.writeValueAsString(
                listOf(
                    mapOf(
                        "dato" to "2020-01-01",
                        "dagtype" to "SYKEDAG",
                        "grad" to 100
                    ),
                    mapOf(
                        "dato" to "2020-01-02",
                        "dagtype" to "SYKEDAG",
                        "grad" to 100
                    )
                )
            )
        )
        val overstyringCommand = OverstyringCommand(testRapid, overstyringMessage)
        sessionOf(dataSource, returnGeneratedKey = true).use(overstyringCommand::execute)

        val rapidmelding = testRapid.inspektør.message(0)

        assertEquals("overstyr_dager", rapidmelding["@event_name"].asText())
        assertEquals(overstyringMessage.aktørId, rapidmelding["aktørId"].asText())
        assertEquals(overstyringMessage.fødselsnummer, rapidmelding["fødselsnummer"].asText())
        assertEquals(overstyringMessage.organisasjonsnummer, rapidmelding["organisasjonsnummer"].asText())
        assertEquals(overstyringMessage.dager, rapidmelding["dager"].asText())
    }
}
