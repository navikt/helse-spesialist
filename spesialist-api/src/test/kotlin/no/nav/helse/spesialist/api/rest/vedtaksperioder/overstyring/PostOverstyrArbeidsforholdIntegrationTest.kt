package no.nav.helse.spesialist.api.rest.vedtaksperioder.overstyring

import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PostOverstyrArbeidsforholdIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    @Test
    fun `overstyrer arbeidsforhold`() {
        // Given:
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()
        val skjæringstidspunkt = LocalDate.of(2021, 1, 1)

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
                url = "/api/vedtaksperioder/$vedtaksperiodeId/overstyringer/arbeidsforhold",
                body =
                    """
                    {
                        "skjæringstidspunkt": "$skjæringstidspunkt",
                        "overstyrteArbeidsforhold": [
                            {
                                "organisasjonsnummer": "$organisasjonsnummer",
                                "deaktivert": true,
                                "begrunnelse": "begrunnelse",
                                "forklaring": "forklaring",
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
        val overstyring = totrinnsvurdering.overstyringer.single() as OverstyrtArbeidsforhold
        assertEquals(saksbehandler.id, overstyring.saksbehandlerOid)
        assertEquals(fødselsnummer, overstyring.fødselsnummer)
        assertEquals(person.aktørId, overstyring.aktørId)
        assertEquals(vedtaksperiodeId, overstyring.vedtaksperiodeId)
        assertEquals(skjæringstidspunkt, overstyring.skjæringstidspunkt)
        assertEquals(1, overstyring.overstyrteArbeidsforhold.size)
        assertEquals(organisasjonsnummer, overstyring.overstyrteArbeidsforhold[0].organisasjonsnummer)
        assertEquals(true, overstyring.overstyrteArbeidsforhold[0].deaktivert)

        assertEquals(saksbehandler, sessionContext.reservasjonDao.data[fødselsnummer]?.reservertTil)

        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse =
                    OverstyrtArbeidsforholdEvent(
                        eksternHendelseId = overstyring.eksternHendelseId,
                        fødselsnummer = fødselsnummer,
                        aktørId = person.aktørId,
                        saksbehandlerOid = saksbehandler.id.value,
                        saksbehandlerNavn = saksbehandler.navn,
                        saksbehandlerIdent = saksbehandler.ident.value,
                        saksbehandlerEpost = saksbehandler.epost,
                        skjæringstidspunkt = skjæringstidspunkt,
                        overstyrteArbeidsforhold =
                            listOf(
                                OverstyrtArbeidsforholdEvent.Arbeidsforhold(
                                    orgnummer = organisasjonsnummer,
                                    deaktivert = true,
                                    begrunnelse = "begrunnelse",
                                    forklaring = "forklaring",
                                ),
                            ),
                    ),
                årsak = "overstyring av arbeidsforhold",
            ),
        )
    }

    @Test
    fun `vedtaksperiode ikke funnet gir 404`() {
        val saksbehandler = lagSaksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()

        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/overstyringer/arbeidsforhold",
                body =
                    """
                    {
                        "skjæringstidspunkt": "2021-01-01",
                        "overstyrteArbeidsforhold": []
                    }
                    """.trimIndent(),
                saksbehandler = saksbehandler,
            )

        assertEquals(404, response.status)
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
