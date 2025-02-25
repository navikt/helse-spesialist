package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AnnulleringMutationHandlerTest : AbstractGraphQLApiTest() {
    @Test
    fun `annullering ok`() {
        val body =
            runQuery(
                """
            mutation Annuller {
                annuller(annullering: {
                    organisasjonsnummer: "et-organisasjonsnummer", 
                    fodselsnummer: "et-fødselsnummer", 
                    aktorId: "en-aktørid", 
                    utbetalingId: "${UUID.randomUUID()}",
                    arbeidsgiverFagsystemId: "EN-FAGSYSTEMID",
                    personFagsystemId: "EN-FAGSYSTEMID",
                    vedtaksperiodeId: "${UUID.randomUUID()}",
                    kommentar: "En kommentar", 
                    begrunnelser: ["Det første", "Det andre"],
                    arsaker: [{_key: "en key", arsak: "Ferie"}]
                })
            }
        """,
            )

        assertTrue(body["data"]["annuller"].asBoolean())
    }

    @Test
    fun `annullering av tomme verdier`() {
        val body =
            runQuery(
                """
            mutation Annuller {
                annuller(annullering: {
                    organisasjonsnummer: "et-organisasjonsnummer", 
                    fodselsnummer: "et-fødselsnummer", 
                    aktorId: "en-aktørid", 
                    utbetalingId: "${UUID.randomUUID()}",
                    arbeidsgiverFagsystemId: "EN-FAGSYSTEMID",
                    personFagsystemId: "EN-FAGSYSTEMID",
                    vedtaksperiodeId: "${UUID.randomUUID()}",
                    begrunnelser: [],
                    arsaker: []
                })
            }
        """,
            )

        assertTrue(body["data"]["annuller"].asBoolean())
    }
}
