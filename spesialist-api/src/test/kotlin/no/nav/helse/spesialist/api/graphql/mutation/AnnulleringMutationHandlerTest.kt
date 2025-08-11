package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class AnnulleringMutationHandlerTest {

    @Test
    fun `annullering ok`() {
        runQuery(
            given = {},
            whenever = annullerMutation(
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
            then = { _, body, _ ->
                assertTrue(body["data"]["annuller"].asBoolean())
            }
        )
    }

    @Test
    fun `annullering av tomme verdier`() {
        runQuery(
            whenever = annullerMutation(
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
            then = { _, body, _ ->
                assertTrue(body["data"]["annuller"].asBoolean())
            }
        )
    }
}
