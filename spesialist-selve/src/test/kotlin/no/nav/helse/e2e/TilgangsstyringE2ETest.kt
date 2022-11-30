package no.nav.helse.e2e

import AbstractE2ETest
import graphql.GraphQLError
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.Meldingssender.sendArbeidsforholdløsningOld
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsningOld
import no.nav.helse.Meldingssender.sendEgenAnsattløsningOld
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsningComposite
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_MED_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilgangsstyringE2ETest : AbstractE2ETest() {

    @Test
    fun `Tar høyde for skjermet-status`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS

        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>("tilganger") } returns mockk(relaxed = true) {
            every { harTilgangTilKode7() } returns false
            every { harTilgangTilSkjermedePersoner() } returns false
        }
        every { dataFetchingEnvironment.graphQlContext.get<String>("saksbehandlerNavn") } returns "saksbehandler"

        val godkjenningsmeldingId = sendMeldingerOppTilEgenAnsatt()

        fetchPerson().let { response ->
            assertFeilmelding("Har ikke tilgang til person med fødselsnummer ", response.errors)
            assertNull(response.data)
        }

        sendEgenAnsattløsningOld(godkjenningsmeldingId = godkjenningsmeldingId, erEgenAnsatt = true)
        fetchPerson().let { response ->
            assertFeilmelding("Har ikke tilgang til person med fødselsnummer ", response.errors)
            assertNull(response.data)
        }

        every { dataFetchingEnvironment.graphQlContext.get<SaksbehandlerTilganger>("tilganger") } returns mockk(relaxed = true) {
            every { harTilgangTilSkjermedePersoner() } returns true
        }
        fetchPerson().let { response ->
            assertEquals(emptyList<GraphQLError>(), response.errors)
            assertNotNull(response.data)
        }
    }

    private fun fetchPerson() = personQuery.person(FØDSELSNUMMER, null, dataFetchingEnvironment)

    private fun sendMeldingerOppTilEgenAnsatt(): UUID {
        val godkjenningsmeldingId = sendGodkjenningsbehov(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsningComposite(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsningOld(
            hendelseId = godkjenningsmeldingId,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            navn = "En Arbeidsgiver",
            bransjer = listOf("En eller flere bransjer")
        )
        sendArbeidsforholdløsningOld(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        return godkjenningsmeldingId
    }

    companion object {
        private fun assertFeilmelding(feilmelding: String, errors: List<GraphQLError>) {
            assertTrue(errors.any { it.message.contains(feilmelding) }) {
                "Forventet at $errors skulle inneholde \"$feilmelding\""
            }
        }
    }
}
