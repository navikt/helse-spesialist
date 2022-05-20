package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilgangsstyringE2ETest : AbstractE2ETest() {

    @Test
    fun `Tar høyde for skjermet-status`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        every { dataFetchingEnvironment.graphQlContext.get<Boolean>("kanSeKode7") } returns false
        every { dataFetchingEnvironment.graphQlContext.get<Boolean>("kanSeSkjermedePersoner") } returns false
        every { dataFetchingEnvironment.graphQlContext.get<String>("saksbehandlerNavn") } returns "saksbehandler"

        val godkjenningsmeldingId = sendMeldingerOppTilEgenAnsatt()

        fetchPerson().let { response ->
            assertTrue(response.errors.any { it.message.contains("Har ikke tilgang til person med fødselsnummer") })
            assertNull(response.data)
        }

        sendEgenAnsattløsning(godkjenningsmeldingId = godkjenningsmeldingId, erEgenAnsatt = true)
        fetchPerson().let { response ->
            assertTrue(response.errors.any { it.message.contains("Har ikke tilgang til person med fødselsnummer") })
            assertNull(response.data)
        }

        every { dataFetchingEnvironment.graphQlContext.get<Boolean>("kanSeSkjermedePersoner") } returns true
        fetchPerson().let { response ->
            assertTrue(response.errors.isEmpty())
            assertNotNull(response.data)
        }
    }

    private fun fetchPerson() = personQuery.person(FØDSELSNUMMER, null, dataFetchingEnvironment)

    private fun sendMeldingerOppTilEgenAnsatt(): UUID {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        return godkjenningsmeldingId
    }
}
