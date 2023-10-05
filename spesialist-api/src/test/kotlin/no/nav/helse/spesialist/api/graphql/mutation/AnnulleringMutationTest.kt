package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AnnulleringMutationTest : AbstractGraphQLApiTest() {
    @Test
    fun `annullering ok`() {
        val body = runQuery(
            """
            mutation Annuller {
                annuller(annullering: {
                    organisasjonsnummer: "et-organisasjonsnummer", 
                    fodselsnummer: "et-fødselsnummer", 
                    aktorId: "en-aktørid", 
                    fagsystemId: "en-fagsystemid",
                    kommentar: "En kommentar", 
                    begrunnelser: ["Det første", "Det andre"]
                })
            }
        """
        )

        assertTrue(body["data"]["annuller"].asBoolean())
    }

    @Test
    fun `annullering av tomme verdier`() {
        val body = runQuery(
            """
            mutation Annuller {
                annuller(annullering: {
                    organisasjonsnummer: "et-organisasjonsnummer", 
                    fodselsnummer: "et-fødselsnummer", 
                    aktorId: "en-aktørid", 
                    fagsystemId: "en-fagsystemid",
                    begrunnelser: []
                })
            }
        """
        )

        assertTrue(body["data"]["annuller"].asBoolean())
    }
}
