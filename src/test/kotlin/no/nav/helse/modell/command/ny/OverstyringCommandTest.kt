package no.nav.helse.modell.command.ny

import junit.framework.Assert.assertTrue
import kotliquery.sessionOf
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import no.nav.helse.modell.overstyring.OverstyringCommand
import no.nav.helse.objectMapper
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class OverstyringCommandTest {
    @Test
    fun `overstyring-command legger ovserstyringsmelding på rapid`() {
        val dataSource = setupDataSourceMedFlyway()

        val overstyringMessage = OverstyringMessage(
            saksbehandlerEpost = "tbd@nav.no",
            saksbehandlerOid = UUID.randomUUID(),
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
        val overstyringCommand = OverstyringCommand(UUID.randomUUID(), null)
        val resultat = sessionOf(dataSource, returnGeneratedKey = true).use {
            overstyringCommand.resume(it, Løsninger().apply {
                add(overstyringMessage)
            })
            overstyringCommand.execute(it)
        }
        assertTrue(resultat is Command.Resultat.Ok.Løst)

        val løsning = (resultat as Command.Resultat.Ok.Løst).løsning

        assertEquals("overstyr_dager", løsning["@event_name"])
        assertEquals(overstyringMessage.aktørId, løsning["aktørId"])
        assertEquals(overstyringMessage.fødselsnummer, løsning["fødselsnummer"])
        assertEquals(overstyringMessage.organisasjonsnummer, løsning["organisasjonsnummer"])
        assertEquals(overstyringMessage.dager, løsning["dager"])
    }
}
