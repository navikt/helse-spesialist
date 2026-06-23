package no.nav.helse.spesialist.api.rest.behandlingsstatistikk

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.rest.ApiBehandlingsstatistikkResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBehandlingsstatistikkBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun lagBehandlingsstatistikk() {
        // When:
        val response = integrationTestFixture.get(url = "/api/behandlingsstatistikk")

        // Then:
        response.body<ApiBehandlingsstatistikkResponse>().run {
            assertEquals(0, enArbeidsgiver.automatisk)
            assertEquals(6, utbetalingTilArbeidsgiver.manuelt)
            assertEquals(5, utbetalingTilArbeidsgiver.tilgjengelig)
            assertEquals(7, antallAnnulleringer)
        }
    }
}
