package no.nav.helse.modell.command.ny

import junit.framework.Assert.assertTrue
import kotliquery.sessionOf
import no.nav.helse.TestPerson
import no.nav.helse.mediator.kafka.meldinger.Dagtype
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import no.nav.helse.modell.overstyring.OverstyringCommand
import no.nav.helse.modell.overstyring.finnOverstyring
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class OverstyringCommandTest {
    val dataSource = setupDataSourceMedFlyway()

    @Test
    fun `overstyring-command legger overstyringsmelding på rapid`() {
        val testPerson = TestPerson(dataSource)
        val eventIdFraSpleis = UUID.randomUUID()
        testPerson.sendGodkjenningMessage(eventIdFraSpleis)
        testPerson.sendPersoninfo(eventIdFraSpleis)
        val overstyringMessage = OverstyringMessage(
            saksbehandlerEpost = "tbd@nav.no",
            saksbehandlerOid = UUID.randomUUID(),
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            begrunnelse = "En god grunn",
            unntaFraInnsyn = false,
            dager = listOf(
                OverstyringMessage.OverstyringMessageDag(
                    dato = LocalDate.of(2020, 1, 1),
                    type = Dagtype.Sykedag,
                    grad = 100
                ),
                OverstyringMessage.OverstyringMessageDag(
                    dato = LocalDate.of(2020, 1, 2),
                    type = Dagtype.Sykedag,
                    grad = 100
                )
            )
        )

        val eventIdFraSaksbehandler = UUID.randomUUID()
        val overstyringCommand = OverstyringCommand(eventIdFraSaksbehandler, null, rapidsConnection = testPerson.rapid)
        val resultat = sessionOf(dataSource, returnGeneratedKey = true).use {
            overstyringCommand.resume(it, Løsninger().apply {
                add(overstyringMessage)
            })
            overstyringCommand.execute(it)
        }
        assertTrue(resultat is Command.Resultat.Ok.Løst)

        val løsning = (resultat as Command.Resultat.Ok.Løst).løsning

        assertEquals("overstyr_tidslinje", løsning["@event_name"])
        assertEquals(overstyringMessage.aktørId, løsning["aktørId"])
        assertEquals(overstyringMessage.fødselsnummer, løsning["fødselsnummer"])
        assertEquals(overstyringMessage.organisasjonsnummer, løsning["organisasjonsnummer"])
        assertEquals(overstyringMessage.dager, løsning["dager"])

        val overstyringer = sessionOf(dataSource).use {
            it.finnOverstyring(testPerson.fødselsnummer, testPerson.orgnummer)
        }

        assertEquals(eventIdFraSaksbehandler, overstyringer.first().hendelseId)
        assertEquals(overstyringer.first().overstyrteDager.size, 2)
    }
}
