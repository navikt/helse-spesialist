package no.nav.helse.spesialist.api.graphql.mutation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID

class AnnulleringMutationIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()

    @Test
    fun `annullering ok`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = mapOf("årsak-1" to "Ferie", "årsak-2" to "Ekstra ferie")

        // When:
        val responseJson = integrationTestFixture.executeQuery(
            saksbehandler = saksbehandler,
            query = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = organisasjonsnummer,
                    fodselsnummer = fødselsnummer,
                    aktorId = aktørId,
                    utbetalingId = utbetalingId,
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    kommentar = kommentar,
                    arsaker = årsaker.map { ApiAnnulleringData.ApiAnnulleringArsak(_key = it.key, arsak = it.value) }
                ),
            )
        )

        // Then:
        // Sjekk svaret
        assertEquals(true, responseJson.get("data")?.get("annuller")?.asBoolean())

        // Sjekk persistert data
        val lagretAnnullering =
            integrationTestFixture
                .daos
                .annulleringRepository
                .finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)

        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.ident, lagretAnnullering.saksbehandlerIdent)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(årsaker.values.sorted(), lagretAnnullering.arsaker.sorted())
        assertEquals(kommentar, lagretAnnullering.begrunnelse)
        assertEquals(vedtaksperiodeId, lagretAnnullering.vedtaksperiodeId)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse = AnnullertUtbetalingEvent(
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer,
                    saksbehandlerOid = saksbehandler.id().value,
                    saksbehandlerNavn = saksbehandler.navn,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingId = utbetalingId,
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    begrunnelser = årsaker.values.toList(),
                    arsaker = årsaker.map { AnnulleringArsak(key = it.key, arsak = it.value) },
                    kommentar = kommentar
                ),
                årsak = "annullering av utbetaling"
            )
        )
    }

    @Test
    fun `annullering av tomme verdier`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        // When:
        val responseJson = integrationTestFixture.executeQuery(
            saksbehandler = saksbehandler,
            query = annullerMutation(
                ApiAnnulleringData(
                    organisasjonsnummer = organisasjonsnummer,
                    fodselsnummer = fødselsnummer,
                    aktorId = aktørId,
                    utbetalingId = utbetalingId,
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    kommentar = kommentar,
                    arsaker = emptyList(),
                )
            ),
        )

        // Then:
        // Sjekk svaret
        assertEquals(true, responseJson.get("data")?.get("annuller")?.asBoolean())

        // Sjekk persistert data
        val lagretAnnullering =
            integrationTestFixture
                .daos
                .annulleringRepository
                .finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)

        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.ident, lagretAnnullering.saksbehandlerIdent)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(emptyList<String>(), lagretAnnullering.arsaker)
        assertEquals(kommentar, lagretAnnullering.begrunnelse)
        assertEquals(vedtaksperiodeId, lagretAnnullering.vedtaksperiodeId)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse = AnnullertUtbetalingEvent(
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer,
                    saksbehandlerOid = saksbehandler.id().value,
                    saksbehandlerNavn = saksbehandler.navn,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingId = utbetalingId,
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    begrunnelser = emptyList(),
                    arsaker = emptyList(),
                    kommentar = kommentar
                ),
                årsak = "annullering av utbetaling"
            )
        )
    }

    @Test
    fun `annullering allerede lagret`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val aktørId = lagAktørId()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = mapOf("årsak-1" to "Ferie", "årsak-2" to "Ekstra ferie")
        integrationTestFixture
            .daos
            .annulleringRepository
            .lagreAnnullering(
                annulleringDto = AnnulleringDto(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingId = utbetalingId,
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    årsaker = årsaker.map { AnnulleringArsak(key = it.key, arsak = it.value) },
                    kommentar = kommentar
                ),
                saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler)
            )

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
                    vedtaksperiodeId = UUID.randomUUID(),
                    kommentar = "En annen kommentar",
                    arsaker = listOf(ApiAnnulleringData.ApiAnnulleringArsak(_key = "ny", arsak = "Helt ny årsak"))
                ),
            )
        )

        // Then:
        // Sjekk svaret
        assertEqualsJson(
            """
              {
                "errors": [
                  {
                    "message": "Exception while fetching data (/annuller) : allerede_annullert",
                    "locations": [ { "line": 3, "column": 9 } ],
                    "path": [ "annuller" ]
                  }
                ]
              }
            """.trimIndent(),
            responseJson
        )

        // Sjekk persistert data
        val lagretAnnullering =
            integrationTestFixture
                .daos
                .annulleringRepository
                .finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)

        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.ident, lagretAnnullering.saksbehandlerIdent)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(årsaker.values.sorted(), lagretAnnullering.arsaker.sorted())
        assertEquals(kommentar, lagretAnnullering.begrunnelse)
        assertEquals(vedtaksperiodeId, lagretAnnullering.vedtaksperiodeId)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser()
    }

    private fun assertEqualsJson(
        expectedJson: String,
        actualJsonNode: JsonNode
    ) {
        val objectMapper = jacksonObjectMapper()
        assertEquals(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(expectedJson)),
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualJsonNode)
        )
    }
}
