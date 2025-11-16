package no.nav.helse.spesialist.api.rest

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID

class PostVedtaksperiodeAnnullerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val annulleringRepository = sessionContext.annulleringRepository

    @Test
    fun `annullering ok`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = mapOf("årsak-1" to "Ferie", "årsak-2" to "Ekstra ferie")

        val person = lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)

        lagVedtaksperiode(
            id = VedtaksperiodeId(vedtaksperiodeId),
            identitetsnummer = person.id,
            organisasjonsnummer = organisasjonsnummer,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtaksperioder/${vedtaksperiodeId}/annuller",
            body = """
                {
                    "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                    "personFagsystemId": "$personFagsystemId",
                    "kommentar": "$kommentar",
                    "årsaker": [
                      ${årsaker.map { (key, arsak) -> """{ "key": "$key", "årsak": "$arsak" }""" }.joinToString()}
                    ]
                }
            """.trimIndent(),
            saksbehandler = saksbehandler,
        )

        // Then:
        // Sjekk svaret
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Sjekk persistert data
        val lagretAnnullering =
            annulleringRepository
                .finnAnnulleringMedEnAv(arbeidsgiverFagsystemId, personFagsystemId)

        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.id, lagretAnnullering.saksbehandlerOid)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(årsaker.values.sorted(), lagretAnnullering.årsaker.sorted())
        assertEquals(kommentar, lagretAnnullering.kommentar)
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
                    organisasjonsnummer = organisasjonsnummer,
                    saksbehandlerOid = saksbehandler.id.value,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    vedtaksperiodeId = vedtaksperiodeId,
                    begrunnelser = årsaker.values.toList(),
                    arsaker = årsaker.map { AnnullertUtbetalingEvent.Årsak(key = it.key, arsak = it.value) },
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
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = emptyList<ApiVedtaksperiodeAnnullerRequest.Årsak>()

        val person = lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)

        lagVedtaksperiode(
            id = VedtaksperiodeId(vedtaksperiodeId),
            identitetsnummer = person.id,
            organisasjonsnummer = organisasjonsnummer,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtaksperioder/${vedtaksperiodeId}/annuller",
            body = """
                {
                    "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                    "personFagsystemId": "$personFagsystemId",
                    "kommentar": "$kommentar",
                    "årsaker": [
                      ${årsaker.joinToString { (key, arsak) -> """{ "key": "$key", "årsak": "$arsak" }""" }}
                    ]
                }
            """.trimIndent(),
            saksbehandler = saksbehandler,
        )

        // Then:
        // Sjekk svaret
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Sjekk persistert data
        val lagretAnnullering =
            annulleringRepository
                .finnAnnulleringMedEnAv(arbeidsgiverFagsystemId, personFagsystemId)

        assertNotNull(lagretAnnullering)

        assertEquals(saksbehandler.id, lagretAnnullering.saksbehandlerOid)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(emptyList<String>(), lagretAnnullering.årsaker)
        assertEquals(kommentar, lagretAnnullering.kommentar)
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
                    organisasjonsnummer = organisasjonsnummer,
                    saksbehandlerOid = saksbehandler.id.value,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    vedtaksperiodeId = vedtaksperiodeId,
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
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tidligereSaksbehandler = lagSaksbehandler()
        val saksbehandler = lagSaksbehandler()

        val arbeidsgiverFagsystemId = "EN_ARBEIDSGIVER_FAGSYSTEM_ID"
        val kommentar = "kommentar"
        val personFagsystemId = "EN_PERSON_FAGSYSTEM_ID"
        val vedtaksperiodeId = UUID.randomUUID()
        val tidligereÅrsaker = mapOf("årsak-1" to "Ferie", "årsak-2" to "Ekstra ferie")
        val årsaker = mapOf("årsak-3" to "Ny ferie", "årsak-4" to "Ny ferie")

        val person = lagPerson(
            id = Identitetsnummer.fraString(fødselsnummer)
        ).also(sessionContext.personRepository::lagre)

        lagVedtaksperiode(
            id = VedtaksperiodeId(vedtaksperiodeId),
            identitetsnummer = person.id,
            organisasjonsnummer = organisasjonsnummer,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        annulleringRepository
            .lagreAnnullering(
                annullering = Annullering.Factory.ny(
                    arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                    personFagsystemId = personFagsystemId,
                    saksbehandlerOid = tidligereSaksbehandler.id,
                    vedtaksperiodeId = vedtaksperiodeId,
                    årsaker = tidligereÅrsaker.map { it.value },
                    kommentar = kommentar
                )
            )

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtaksperioder/${vedtaksperiodeId}/annuller",
            body = """
                {
                    "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                    "personFagsystemId": "$personFagsystemId",
                    "kommentar": "$kommentar",
                    "årsaker": [
                      ${årsaker.map { (key, arsak) -> """{ "key": "$key", "årsak": "$arsak" }""" }.joinToString()}
                    ]
                }
            """.trimIndent(),
            saksbehandler = saksbehandler,
        )

        // Then:
        // Sjekk svaret
        assertEquals(409, response.status)
        integrationTestFixture.assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 409,
              "title": "Perioden er allerede annullert",
              "code": "ALLEREDE_ANNULLERT" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )

        // Sjekk persistert data
        val lagretAnnullering =
            annulleringRepository
                .finnAnnulleringMedEnAv(arbeidsgiverFagsystemId, personFagsystemId)

        assertNotNull(lagretAnnullering)

        assertEquals(tidligereSaksbehandler.id, lagretAnnullering.saksbehandlerOid)
        assertEquals(arbeidsgiverFagsystemId, lagretAnnullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, lagretAnnullering.personFagsystemId)
        assertEquals(tidligereÅrsaker.values.sorted(), lagretAnnullering.årsaker.sorted())
        assertEquals(kommentar, lagretAnnullering.kommentar)
        assertEquals(vedtaksperiodeId, lagretAnnullering.vedtaksperiodeId)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser()
    }

}
