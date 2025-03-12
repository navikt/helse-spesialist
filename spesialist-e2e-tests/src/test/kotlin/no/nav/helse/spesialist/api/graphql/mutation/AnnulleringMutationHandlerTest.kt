package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AnnulleringMutationHandlerTest : AbstractGraphQLApiTest() {
    @Test
    fun `annullering ok`() {
        val body =
            runQuery(
                annullerMutation(
                    ApiAnnulleringData(
                        organisasjonsnummer = lagOrganisasjonsnummer(),
                        fodselsnummer = lagFødselsnummer(),
                        aktorId = lagAktørId(),
                        utbetalingId = UUID.randomUUID(),
                        arbeidsgiverFagsystemId = "EN-FAGSYSTEMID",
                        personFagsystemId = "EN-FAGSYSTEMID",
                        vedtaksperiodeId = UUID.randomUUID(),
                        kommentar = "kommentar",
                        begrunnelser = listOf("Det første", "Det andre"),
                        arsaker = listOf(
                            ApiAnnulleringData.ApiAnnulleringArsak(
                                _key = "en key",
                                arsak = "Ferie",
                            )
                        ),
                    )
                ),
            )

        assertTrue(body["data"]["annuller"].asBoolean())
    }

    @Test
    fun `annullering av tomme verdier`() {
        val body =
            runQuery(
                annullerMutation(
                    ApiAnnulleringData(
                        organisasjonsnummer = lagOrganisasjonsnummer(),
                        fodselsnummer = lagFødselsnummer(),
                        aktorId = lagAktørId(),
                        utbetalingId = UUID.randomUUID(),
                        arbeidsgiverFagsystemId = "EN-FAGSYSTEMID",
                        personFagsystemId = "EN-FAGSYSTEMID",
                        vedtaksperiodeId = UUID.randomUUID(),
                        kommentar = "kommentar",
                        begrunnelser = emptyList(),
                        arsaker = emptyList()
                    )
                ),
            )

        assertTrue(body["data"]["annuller"].asBoolean())
    }
}
