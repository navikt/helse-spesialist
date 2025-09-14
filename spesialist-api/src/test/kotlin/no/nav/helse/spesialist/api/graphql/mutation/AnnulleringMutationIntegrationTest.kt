package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID

class AnnulleringMutationIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    private val annulleringRepository = integrationTestFixture.daos.annulleringRepository

    @Test
    fun `annullering ok`() {
        // Given:
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = listOf("Ferie", "Ekstra ferie")

        // When:
        val responseJson = integrationTestFixture.executeQuery(
            saksbehandler = saksbehandler,
            query = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    fodselsnummer = lagFødselsnummer(),
                    aktorId = lagAktørId(),
                    utbetalingId = UUID.randomUUID(),
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    kommentar = kommentar,
                    arsaker = årsaker.mapIndexed { index, årsak ->
                        ApiAnnulleringData.ApiAnnulleringArsak(
                            _key = "årsak-$index",
                            arsak = årsak,
                        )
                    }
                ),
            )
        )

        // Then:
        assertEquals(true, responseJson.get("data")?.get("annuller")?.asBoolean())

        val lagretAnnullering = annulleringRepository.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.ident, lagretAnnullering.saksbehandlerIdent)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(årsaker.sorted(), lagretAnnullering.arsaker.sorted())
        assertEquals(kommentar, lagretAnnullering.begrunnelse)
        assertEquals(vedtaksperiodeId, lagretAnnullering.vedtaksperiodeId)
    }

    @Test
    fun `annullering av tomme verdier`() {
        // Given:
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val vedtaksperiodeId = UUID.randomUUID()

        // When:
        val responseJson = integrationTestFixture.executeQuery(
            saksbehandler = saksbehandler,
            query = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = lagOrganisasjonsnummer(),
                    fodselsnummer = lagFødselsnummer(),
                    aktorId = lagAktørId(),
                    utbetalingId = UUID.randomUUID(),
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    kommentar = kommentar,
                    arsaker = emptyList(),
                )
            ),
        )

        // Then:
        assertEquals(true, responseJson.get("data")?.get("annuller")?.asBoolean())

        val lagretAnnullering = annulleringRepository.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.ident, lagretAnnullering.saksbehandlerIdent)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(emptyList<String>(), lagretAnnullering.arsaker)
        assertEquals(kommentar, lagretAnnullering.begrunnelse)
        assertEquals(vedtaksperiodeId, lagretAnnullering.vedtaksperiodeId)
    }
}
