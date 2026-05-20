package no.nav.helse.spesialist.api.rest

import no.nav.helse.modell.melding.AnmodningOmForkastingEvent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PostAnmodOmForkastingIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `anmodning om forkasting ok`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = mapOf("årsak-1" to "Feil periode", "årsak-2" to "Manglende dokumentasjon")
        val kommentar = "En kommentar"

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(
                id = VedtaksperiodeId(vedtaksperiodeId),
                identitetsnummer = person.id,
                organisasjonsnummer = organisasjonsnummer,
            ).also(sessionContext.vedtaksperiodeRepository::lagre)

        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also(sessionContext.behandlingRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/anmod-om-forkasting",
                body =
                    """
                    {
                        "kommentar": "$kommentar",
                        "årsaker": [
                          ${årsaker.map { (key, arsak) -> """{ "key": "$key", "årsak": "$arsak" }""" }.joinToString()}
                        ]
                    }
                    """.trimIndent(),
                saksbehandler = saksbehandler,
                brukerroller = setOf(Brukerrolle.SelvstendigNæringsdrivendeBeta),
            )

        // Then:
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse =
                    AnmodningOmForkastingEvent(
                        fødselsnummer = fødselsnummer,
                        vedtaksperiodeId = vedtaksperiodeId.toString(),
                        organisasjonsnummer = organisasjonsnummer,
                        yrkesaktivitetstype = behandling.yrkesaktivitetstype.toString(),
                        årsaker = årsaker.values.toList(),
                        kommentar = kommentar,
                    ),
                årsak = "anmodning om forkasting av vedtaksperiode",
            ),
        )
    }

    @Test
    fun `anmodning om forkasting med tomme årsaker gir 400`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(
                id = VedtaksperiodeId(vedtaksperiodeId),
                identitetsnummer = person.id,
                organisasjonsnummer = organisasjonsnummer,
            ).also(sessionContext.vedtaksperiodeRepository::lagre)

        lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
            .also(sessionContext.behandlingRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/anmod-om-forkasting",
                body = """{ "årsaker": [], "kommentar": "En kommentar" }""",
                saksbehandler = saksbehandler,
                brukerroller = setOf(Brukerrolle.SelvstendigNæringsdrivendeBeta),
            )

        // Then:
        assertEquals(400, response.status)
    }

    @Test
    fun `anmodning om forkasting uten kommentar er ok`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()
        val årsaker = mapOf("årsak-1" to "Feil periode")

        val person =
            lagPerson(
                id = Identitetsnummer.fraString(fødselsnummer),
            ).also(sessionContext.personRepository::lagre)

        val vedtaksperiode =
            lagVedtaksperiode(
                id = VedtaksperiodeId(vedtaksperiodeId),
                identitetsnummer = person.id,
                organisasjonsnummer = organisasjonsnummer,
            ).also(sessionContext.vedtaksperiodeRepository::lagre)

        val behandling =
            lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
                .also(sessionContext.behandlingRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/anmod-om-forkasting",
                body = """{ "årsaker": [{ "key": "årsak-1", "årsak": "Feil periode" }], "kommentar": null }""",
                saksbehandler = saksbehandler,
                brukerroller = setOf(Brukerrolle.SelvstendigNæringsdrivendeBeta),
            )

        // Then:
        assertEquals(204, response.status)

        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse =
                    AnmodningOmForkastingEvent(
                        fødselsnummer = fødselsnummer,
                        vedtaksperiodeId = vedtaksperiodeId.toString(),
                        organisasjonsnummer = organisasjonsnummer,
                        yrkesaktivitetstype = behandling.yrkesaktivitetstype.toString(),
                        årsaker = årsaker.values.toList(),
                        kommentar = null,
                    ),
                årsak = "anmodning om forkasting av vedtaksperiode",
            ),
        )
    }
}
