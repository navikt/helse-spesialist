package no.nav.helse.spesialist.api.rest.vedtaksperioder.overstyring

import no.nav.helse.modell.melding.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PostOverstyrInntektOgRefusjonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    @Test
    fun `overstyrer inntekt og refusjon`() {
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
                url = "/api/vedtaksperioder/$vedtaksperiodeId/overstyringer/inntekt-og-refusjon",
                body =
                    """
                    {
                        "skjæringstidspunkt": "$skjæringstidspunkt",
                        "arbeidsgivere": [
                            {
                                "organisasjonsnummer": "$organisasjonsnummer",
                                "månedligInntekt": 25000.0,
                                "fraMånedligInntekt": 25001.0,
                                "refusjonsopplysninger": null,
                                "fraRefusjonsopplysninger": null,
                                "begrunnelse": "begrunnelse",
                                "forklaring": "testbortforklaring",
                                "lovhjemmel": null,
                                "fom": null,
                                "tom": null
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
        val overstyring = totrinnsvurdering.overstyringer.single() as OverstyrtInntektOgRefusjon
        assertEquals(saksbehandler.id, overstyring.saksbehandlerOid)
        assertEquals(fødselsnummer, overstyring.fødselsnummer)
        assertEquals(person.aktørId, overstyring.aktørId)
        assertEquals(vedtaksperiodeId, overstyring.vedtaksperiodeId)
        assertEquals(skjæringstidspunkt, overstyring.skjæringstidspunkt)
        assertEquals(1, overstyring.arbeidsgivere.size)
        assertEquals(organisasjonsnummer, overstyring.arbeidsgivere[0].organisasjonsnummer)
        assertEquals(25000.0, overstyring.arbeidsgivere[0].månedligInntekt)
        assertEquals(25001.0, overstyring.arbeidsgivere[0].fraMånedligInntekt)

        assertEquals(saksbehandler, sessionContext.reservasjonDao.data[fødselsnummer]?.reservertTil)

        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse =
                    OverstyrtInntektOgRefusjonEvent(
                        eksternHendelseId = overstyring.eksternHendelseId,
                        fødselsnummer = fødselsnummer,
                        aktørId = person.aktørId,
                        skjæringstidspunkt = skjæringstidspunkt,
                        arbeidsgivere =
                            listOf(
                                OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent(
                                    organisasjonsnummer,
                                    25000.0,
                                    25001.0,
                                    refusjonsopplysninger = null,
                                    fraRefusjonsopplysninger = null,
                                    begrunnelse = "begrunnelse",
                                    forklaring = "testbortforklaring",
                                    fom = null,
                                    tom = null,
                                ),
                            ),
                        saksbehandlerOid = saksbehandler.id.value,
                        saksbehandlerNavn = saksbehandler.navn,
                        saksbehandlerIdent = saksbehandler.ident.value,
                        saksbehandlerEpost = saksbehandler.epost,
                    ),
                årsak = "overstyring av inntekt og refusjon",
            ),
        )
    }

    @Test
    fun `mangler tilgang til person gir 403`() {
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

        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat.ManglerTilgang(
                com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler.FortroligAdresse,
            )

        val response =
            integrationTestFixture.post(
                url = "/api/vedtaksperioder/$vedtaksperiodeId/overstyringer/inntekt-og-refusjon",
                body =
                    """
                    {
                        "skjæringstidspunkt": "2021-01-01",
                        "arbeidsgivere": []
                    }
                    """.trimIndent(),
                saksbehandler = saksbehandler,
            )

        assertEquals(403, response.status)
        assertEquals(0, totrinnsvurderingRepository.alle().count { it.fødselsnummer == fødselsnummer })
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
