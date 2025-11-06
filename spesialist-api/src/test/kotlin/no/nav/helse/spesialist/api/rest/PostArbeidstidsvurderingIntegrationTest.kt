package no.nav.helse.spesialist.api.rest

import no.nav.helse.modell.melding.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagMellomnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PostArbeidstidsvurderingIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    private val saksbehandler = lagSaksbehandler()
    private val aktørId = lagAktørId()
    private val fødselsnummer = lagFødselsnummer()

    private val begrunnelse = "En begrunnelse"

    private val arbeidsgiver1Organisasjonsnummer = lagOrganisasjonsnummer()
    private val arbeidsgiver1BerortVedtaksperiodeId = UUID.randomUUID()
    private val arbeidsgiver2Organisasjonsnummer = lagOrganisasjonsnummer()
    private val arbeidsgiver2BerortVedtaksperiodeId = UUID.randomUUID()

    private val vedtaksperiodeId = UUID.randomUUID()

    @Test
    fun `Forbidden dersom personen har adressebeskyttelse som saksbehandler ikke har tilgang til`() {
        // Given:
        lagrePerson(Adressebeskyttelse.Fortrolig)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/personer/$aktørId/vurderinger/arbeidstid",
            saksbehandler = saksbehandler,
            body = """
                {
                    "aktørId": "$aktørId",
                    "fødselsnummer": "$fødselsnummer",
                    "perioderVurdertOk": [],
                    "perioderVurdertIkkeOk": [],
                    "begrunnelse": "$begrunnelse",
                    "arbeidsgivere": [
                        {
                            "organisasjonsnummer": "$arbeidsgiver1Organisasjonsnummer",
                            "berørtVedtaksperiodeId": "$arbeidsgiver1BerortVedtaksperiodeId"
                        },
                        {
                            "organisasjonsnummer": "$arbeidsgiver2Organisasjonsnummer",
                            "berørtVedtaksperiodeId": "$arbeidsgiver2BerortVedtaksperiodeId"
                        }
                    ],
                    "initierendeVedtaksperiodeId": "$vedtaksperiodeId"
                }
            """,
        )

        // Then:

        // Sjekk respons
        assertEquals(403, response.status)
        integrationTestFixture.assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 403,
              "title": "Mangler tilgang til person",
              "code": "MANGLER_TILGANG_TIL_PERSON" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )

        // Sjekk lagret data
        assertEquals(0, totrinnsvurderingRepository.data.values.count { it.fødselsnummer == fødselsnummer })

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Bad request dersom request ikke inneholder noen vurderte perioder`() {
        // Given:
        lagrePerson()

        // When:
        val response = integrationTestFixture.post(
            url = "/api/personer/$aktørId/vurderinger/arbeidstid",
            saksbehandler = saksbehandler,
            body = """
                {
                    "aktørId": "$aktørId",
                    "fødselsnummer": "$fødselsnummer",
                    "perioderVurdertOk": [],
                    "perioderVurdertIkkeOk": [],
                    "begrunnelse": "$begrunnelse",
                    "arbeidsgivere": [
                        {
                            "organisasjonsnummer": "$arbeidsgiver1Organisasjonsnummer",
                            "berørtVedtaksperiodeId": "$arbeidsgiver1BerortVedtaksperiodeId"
                        },
                        {
                            "organisasjonsnummer": "$arbeidsgiver2Organisasjonsnummer",
                            "berørtVedtaksperiodeId": "$arbeidsgiver2BerortVedtaksperiodeId"
                        }
                    ],
                    "initierendeVedtaksperiodeId": "$vedtaksperiodeId"
                }
            """,
        )

        // Then:

        // Sjekk respons
        assertEquals(400, response.status)
        integrationTestFixture.assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Mangler vurderte perioder",
              "code": "MANGLER_VURDERTE_PERIODER" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!
        )

        // Sjekk lagret data
        assertEquals(0, totrinnsvurderingRepository.data.values.count { it.fødselsnummer == fødselsnummer })

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Happy case`() {
        // Given:
        val okPeriode1Fom = "2021-02-03"
        val okPeriode1Tom = "2021-04-05"
        val okPeriode2Fom = "2021-06-07"
        val okPeriode2Tom = "2021-08-09"
        val ikkeOkPeriode1Fom = "2020-02-03"
        val ikkeOkPeriode1Tom = "2020-04-05"
        val ikkeOkPeriode2Fom = "2020-06-07"
        val ikkeOkPeriode2Tom = "2020-08-09"

        lagrePerson()

        // When:
        val response = integrationTestFixture.post(
            url = "/api/personer/$aktørId/vurderinger/arbeidstid",
            saksbehandler = saksbehandler,
            body = """
                {
                    "aktørId": "$aktørId",
                    "fødselsnummer": "$fødselsnummer",
                    "perioderVurdertOk": [
                        { "fom": "$okPeriode1Fom", "tom": "$okPeriode1Tom" },
                        { "fom": "$okPeriode2Fom", "tom": "$okPeriode2Tom" }
                    ],
                    "perioderVurdertIkkeOk": [
                        { "fom": "$ikkeOkPeriode1Fom", "tom": "$ikkeOkPeriode1Tom" },
                        { "fom": "$ikkeOkPeriode2Fom", "tom": "$ikkeOkPeriode2Tom" }
                    ],
                    "begrunnelse": "$begrunnelse",
                    "arbeidsgivere": [
                        {
                            "organisasjonsnummer": "$arbeidsgiver1Organisasjonsnummer",
                            "berørtVedtaksperiodeId": "$arbeidsgiver1BerortVedtaksperiodeId"
                        },
                        {
                            "organisasjonsnummer": "$arbeidsgiver2Organisasjonsnummer",
                            "berørtVedtaksperiodeId": "$arbeidsgiver2BerortVedtaksperiodeId"
                        }
                    ],
                    "initierendeVedtaksperiodeId": "$vedtaksperiodeId"
                }
            """
        )

        // Then:

        // Sjekk respons
        assertEquals(204, response.status)
        assertEquals("", response.bodyAsText)

        // Sjekk lagret data
        val totrinnsvurdering = totrinnsvurderingRepository.data.values.single { it.fødselsnummer == fødselsnummer }
        assertEquals(TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER, totrinnsvurdering.tilstand)

        val overstyring = totrinnsvurdering.overstyringer.single() as MinimumSykdomsgrad
        assertEquals(saksbehandler.id(), overstyring.saksbehandlerOid)
        assertEquals(fødselsnummer, overstyring.fødselsnummer)
        assertEquals(aktørId, overstyring.aktørId)
        assertEquals(vedtaksperiodeId, overstyring.vedtaksperiodeId)
        assertEquals(false, overstyring.ferdigstilt)
        assertEquals(2, overstyring.perioderVurdertOk.size)
        assertEquals(LocalDate.parse(okPeriode1Fom), overstyring.perioderVurdertOk[0].fom)
        assertEquals(LocalDate.parse(okPeriode1Tom), overstyring.perioderVurdertOk[0].tom)
        assertEquals(LocalDate.parse(okPeriode2Fom), overstyring.perioderVurdertOk[1].fom)
        assertEquals(LocalDate.parse(okPeriode2Tom), overstyring.perioderVurdertOk[1].tom)
        assertEquals(2, overstyring.perioderVurdertIkkeOk.size)
        assertEquals(LocalDate.parse(ikkeOkPeriode1Fom), overstyring.perioderVurdertIkkeOk[0].fom)
        assertEquals(LocalDate.parse(ikkeOkPeriode1Tom), overstyring.perioderVurdertIkkeOk[0].tom)
        assertEquals(LocalDate.parse(ikkeOkPeriode2Fom), overstyring.perioderVurdertIkkeOk[1].fom)
        assertEquals(LocalDate.parse(ikkeOkPeriode2Tom), overstyring.perioderVurdertIkkeOk[1].tom)
        assertEquals(begrunnelse, overstyring.begrunnelse)
        assertEquals(2, overstyring.arbeidsgivere.size)
        assertEquals(arbeidsgiver1Organisasjonsnummer, overstyring.arbeidsgivere[0].organisasjonsnummer)
        assertEquals(arbeidsgiver1BerortVedtaksperiodeId, overstyring.arbeidsgivere[0].berørtVedtaksperiodeId)
        assertEquals(arbeidsgiver2Organisasjonsnummer, overstyring.arbeidsgivere[1].organisasjonsnummer)
        assertEquals(arbeidsgiver2BerortVedtaksperiodeId, overstyring.arbeidsgivere[1].berørtVedtaksperiodeId)

        assertEquals(saksbehandler, sessionContext.reservasjonDao.data[fødselsnummer]?.reservertTil)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse = MinimumSykdomsgradVurdertEvent(
                    eksternHendelseId = overstyring.eksternHendelseId,
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId,
                    saksbehandlerOid = saksbehandler.id().value,
                    saksbehandlerNavn = saksbehandler.navn,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    perioderMedMinimumSykdomsgradVurdertOk = overstyring.perioderVurdertOk,
                    perioderMedMinimumSykdomsgradVurdertIkkeOk = overstyring.perioderVurdertIkkeOk,
                ),
                årsak = "vurdering av minimum sykdomsgrad"
            )
        )
        integrationTestFixture.assertPubliserteSubsumsjoner(
            lagPublisertSubsumsjon(
                utfall = "VILKAR_OPPFYLT",
                fom = okPeriode1Fom,
                tom = okPeriode1Tom,
                overstyring = overstyring
            ),
            lagPublisertSubsumsjon(
                utfall = "VILKAR_OPPFYLT",
                fom = okPeriode2Fom,
                tom = okPeriode2Tom,
                overstyring = overstyring
            ),
            lagPublisertSubsumsjon(
                utfall = "VILKAR_IKKE_OPPFYLT",
                fom = ikkeOkPeriode1Fom,
                tom = ikkeOkPeriode1Tom,
                overstyring = overstyring
            ),
            lagPublisertSubsumsjon(
                utfall = "VILKAR_IKKE_OPPFYLT",
                fom = ikkeOkPeriode2Fom,
                tom = ikkeOkPeriode2Tom,
                overstyring = overstyring
            ),
        )
    }

    private fun lagrePerson(adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert) {
        sessionContext.personDao.upsertPersoninfo(
            fødselsnummer = fødselsnummer,
            fornavn = lagFornavn(),
            mellomnavn = lagMellomnavn(),
            etternavn = lagEtternavn(),
            fødselsdato = LocalDate.now(),
            kjønn = Kjønn.Ukjent,
            adressebeskyttelse = adressebeskyttelse
        )
    }

    private fun lagPublisertSubsumsjon(utfall: String, fom: String, tom: String, overstyring: MinimumSykdomsgrad) =
        InMemoryMeldingPubliserer.PublisertSubsumsjon(
            fødselsnummer = fødselsnummer,
            subsumsjonEvent = SubsumsjonEvent(
                id = UUID.randomUUID(), // Blir ikke asserted siden den er autogenerert
                fødselsnummer = fødselsnummer,
                paragraf = "8-13",
                ledd = "1",
                bokstav = null,
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-01-01",
                utfall = utfall,
                input = mapOf(
                    "fom" to LocalDate.parse(fom),
                    "tom" to LocalDate.parse(tom),
                    "initierendeVedtaksperiode" to vedtaksperiodeId,
                ),
                output = emptyMap(),
                sporing = mapOf(
                    "vedtaksperiode" to listOf(
                        arbeidsgiver1BerortVedtaksperiodeId,
                        arbeidsgiver2BerortVedtaksperiodeId
                    ).map(UUID::toString),
                    "organisasjonsnummer" to listOf(
                        arbeidsgiver1Organisasjonsnummer,
                        arbeidsgiver2Organisasjonsnummer
                    ),
                    "saksbehandler" to listOf(saksbehandler.epost),
                    "vurdertMinimumSykdomsgrad" to listOf(overstyring.eksternHendelseId.toString())
                ),
                tidsstempel = LocalDateTime.now(), // Blir ikke asserted siden den er autogenerert
                kilde = "spesialist"
            ),
            versjonAvKode = "0.0.0"
        )
}
