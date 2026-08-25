package no.nav.helse.spesialist.api.rest.vedtaksperioder.overstyring

import no.nav.helse.modell.melding.OverstyrtTidslinjeEvent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate
import java.util.UUID

class PostOverstyrTidslinjeIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    @Test
    fun `overstyrer tidslinje`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()

        val person =
            lagPerson(id = Identitetsnummer.fraString(fødselsnummer))
                .also(sessionContext.personRepository::lagre)

        lagVedtaksperiode(
            id = VedtaksperiodeId(vedtaksperiodeId),
            identitetsnummer = person.id,
            organisasjonsnummer = organisasjonsnummer,
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/overstyringer/tidslinje",
                body =
                    """
                    {
                        "begrunnelse": "en begrunnelse",
                        "dager": [
                            {
                                "dato": "2021-01-20",
                                "type": "Feriedag",
                                "fraType": "Sykedag",
                                "grad": null,
                                "fraGrad": 100,
                                "lovhjemmel": null
                            }
                        ]
                    }
                    """.trimIndent(),
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        val totrinnsvurdering = totrinnsvurderingRepository.alle().single { it.fødselsnummer == fødselsnummer }
        val overstyring = totrinnsvurdering.overstyringer.single() as OverstyrtTidslinje
        assertEquals(saksbehandler.id, overstyring.saksbehandlerOid)
        assertEquals(fødselsnummer, overstyring.fødselsnummer)
        assertEquals(person.aktørId, overstyring.aktørId)
        assertEquals(vedtaksperiodeId, overstyring.vedtaksperiodeId)
        assertEquals(organisasjonsnummer, overstyring.organisasjonsnummer)
        assertEquals("en begrunnelse", overstyring.begrunnelse)
        assertEquals(1, overstyring.dager.size)
        assertEquals(LocalDate.of(2021, 1, 20), overstyring.dager[0].dato)

        assertEquals(saksbehandler, sessionContext.reservasjonDao.data[fødselsnummer]?.reservertTil)

        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse =
                    OverstyrtTidslinjeEvent(
                        eksternHendelseId = overstyring.eksternHendelseId,
                        fødselsnummer = fødselsnummer,
                        aktørId = person.aktørId,
                        organisasjonsnummer = organisasjonsnummer,
                        vedtaksperiodeId = vedtaksperiodeId,
                        dager =
                            listOf(
                                OverstyrtTidslinjeEvent.OverstyrtTidslinjeEventDag(
                                    dato = LocalDate.of(2021, 1, 20),
                                    type = "Feriedag",
                                    fraType = "Sykedag",
                                    grad = null,
                                    fraGrad = 100,
                                ),
                            ),
                    ),
                årsak = "overstyring av tidslinje",
            ),
        )
    }

    @Test
    fun `vedtaksperiode ikke funnet gir 404`() {
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()

        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/overstyringer/tidslinje",
                body =
                    """
                    {
                        "begrunnelse": "en begrunnelse",
                        "dager": []
                    }
                    """.trimIndent(),
                saksbehandler = saksbehandler,
            )

        assertEquals(404, response.status)
        assertNotNull(response.bodyAsJsonNode)
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
