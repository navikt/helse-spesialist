package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.api.testfixtures.mutation.annullerMutation
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
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
        val lagretAnnullering = finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
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
        val lagretAnnullering = finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
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

    private fun finnAnnullering(arbeidsgiverFagsystemId: String, personFagsystemId: String): Annullering? =
        integrationTestFixture
            .daos
            .annulleringRepository
            .finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
}
