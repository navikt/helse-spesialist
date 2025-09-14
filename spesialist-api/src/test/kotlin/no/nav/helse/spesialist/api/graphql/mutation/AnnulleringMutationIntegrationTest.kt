package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class AnnulleringMutationIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `annullering ok`() {
        // When:
        val responseJson = integrationTestFixture.executeQuery(
            query = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    fodselsnummer = lagFødselsnummer(),
                    aktorId = lagAktørId(),
                    utbetalingId = UUID.randomUUID(),
                    arbeidsgiverFagsystemId = "EN-FAGSYSTEMID",
                    personFagsystemId = "EN-FAGSYSTEMID",
                    vedtaksperiodeId = UUID.randomUUID(),
                    kommentar = "kommentar",
                    arsaker = listOf(
                        ApiAnnulleringData.ApiAnnulleringArsak(
                            _key = "en key",
                            arsak = "Ferie",
                        ),
                        ApiAnnulleringData.ApiAnnulleringArsak(
                            _key = "en annen key",
                            arsak = "Ekstra ferie",
                        )
                    ),
                )
            ),
        )

        // Then:
        assertEquals(true, responseJson.get("data")?.get("annuller")?.asBoolean())
    }

    @Test
    fun `annullering av tomme verdier`() {
        // When:
        val responseJson = integrationTestFixture.executeQuery(
            query = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    fodselsnummer = lagFødselsnummer(),
                    aktorId = lagAktørId(),
                    utbetalingId = UUID.randomUUID(),
                    arbeidsgiverFagsystemId = "EN-FAGSYSTEMID",
                    personFagsystemId = "EN-FAGSYSTEMID",
                    vedtaksperiodeId = UUID.randomUUID(),
                    kommentar = "kommentar",
                    arsaker = emptyList(),
                )
            ),
        )

        // Then:
        assertEquals(true, responseJson.get("data")?.get("annuller")?.asBoolean())
    }
}
