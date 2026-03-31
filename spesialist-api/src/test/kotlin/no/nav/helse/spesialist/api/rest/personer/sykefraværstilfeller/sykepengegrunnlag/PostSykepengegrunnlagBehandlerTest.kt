package no.nav.helse.spesialist.api.rest.personer.sykefraværstilfeller.sykepengegrunnlag

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class PostSykepengegrunnlagBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository
    private val totrinnsvurderingRepository = integrationTestFixture.sessionFactory.sessionContext.totrinnsvurderingRepository

    @Test
    fun `happy path`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
                .nyPersonPseudoId(person.id)
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val vedtaksperiodeId = lagVedtaksperiodeId()
        val saksbehandler = lagSaksbehandler()
        val skjæringstidspunkt = 1.jan(2018)
        val begrunnelseMal = "En begrunnelse-mal"
        val begrunnelseFritekst = "En begrunnelse-fritekst"
        val begrunnelseKonklusjon = "En begrunnelse-konklusjon"
        val årsak = "En årsak"

        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${personPseudoId.value}/sykefravaerstilfeller/$skjæringstidspunkt/sykepengegrunnlag""",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "årsak": "$årsak",
                      "sykepengegrunnlagstype": {
                        "discriminatorType": "ApiSkjønnsfastsatt",
                        "skjønnsfastsettelsestype": "OMREGNET_ÅRSINNTEKT",
                        "lovverksreferanse": {
                          "paragraf": "8-30",
                          "ledd": "3",
                          "bokstav": null,
                          "lovverk": "folketrygdloven",
                          "lovverksversjon": "2019-01-01"
                        },
                        "skjønnsfastsatteInntekter": [
                          {
                            "organisasjonsnummer": "$organisasjonsnummer1",
                            "årlig": 500000,
                            "fraÅrlig": 400000
                          },
                          {
                            "organisasjonsnummer": "$organisasjonsnummer2",
                            "årlig": 300000,
                            "fraÅrlig": 200000
                          }
                        ]
                      },
                      "begrunnelseMal": "$begrunnelseMal",
                      "begrunnelseFritekst": "$begrunnelseFritekst",
                      "begrunnelseKonklusjon": "$begrunnelseKonklusjon",
                      "intierendeVedtaksperiodeId": "${vedtaksperiodeId.value}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
        assertNotNull(totrinnsvurdering)
        assertEquals(1, totrinnsvurdering.overstyringer.size)
        val overstyring = totrinnsvurdering.overstyringer.single()
        assertIs<SkjønnsfastsattSykepengegrunnlag>(overstyring)

        assertEquals(skjæringstidspunkt, overstyring.skjæringstidspunkt)
        assertEquals(begrunnelseMal, overstyring.begrunnelseMal)
        assertEquals(begrunnelseFritekst, overstyring.begrunnelseFritekst)
        assertEquals(begrunnelseKonklusjon, overstyring.begrunnelseKonklusjon)
        assertEquals(årsak, overstyring.årsak)
        assertEquals(SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT, overstyring.type)
        assertEquals(false, overstyring.ferdigstilt)
        assertEquals(
            listOf(
                SkjønnsfastsattArbeidsgiver(
                    organisasjonsnummer1,
                    årlig = 500000.0,
                    fraÅrlig = 400000.0,
                ),
                SkjønnsfastsattArbeidsgiver(
                    organisasjonsnummer2,
                    årlig = 300000.0,
                    fraÅrlig = 200000.0,
                ),
            ),
            overstyring.arbeidsgivere,
        )
        assertEquals(
            Lovhjemmel(
                paragraf = "8-30",
                ledd = "3",
                bokstav = null,
                lovverksversjon = "2019-01-01",
                lovverk = "folketrygdloven",
            ),
            overstyring.lovhjemmel,
        )

        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                person.id.value,
                SkjønnsfastsattSykepengegrunnlagEvent(
                    eksternHendelseId = overstyring.eksternHendelseId,
                    fødselsnummer = person.id.value,
                    aktørId = person.aktørId,
                    saksbehandlerOid = saksbehandler.id.value,
                    saksbehandlerNavn = saksbehandler.navn,
                    saksbehandlerIdent = saksbehandler.ident.value,
                    saksbehandlerEpost = saksbehandler.epost,
                    skjæringstidspunkt = skjæringstidspunkt,
                    arbeidsgivere =
                        listOf(
                            SkjønnsfastsattSykepengegrunnlagEvent.SkjønnsfastsattArbeidsgiver(
                                organisasjonsnummer = organisasjonsnummer1,
                                årlig = 500000.0,
                                fraÅrlig = 400000.0,
                            ),
                            SkjønnsfastsattSykepengegrunnlagEvent.SkjønnsfastsattArbeidsgiver(
                                organisasjonsnummer = organisasjonsnummer2,
                                årlig = 300000.0,
                                fraÅrlig = 200000.0,
                            ),
                        ),
                ),
                årsak = "skjønnsfastsetting av sykepengegrunnlag",
            ),
        )
        integrationTestFixture.assertPubliserteSubsumsjoner(
            InMemoryMeldingPubliserer.PublisertSubsumsjon(
                fødselsnummer = person.id.value,
                subsumsjonEvent =
                    SubsumsjonEvent(
                        id = UUID.randomUUID(), // Blir ikke asserted siden den er auto-generert
                        fødselsnummer = person.id.value,
                        paragraf = "8-30",
                        ledd = "3",
                        bokstav = null,
                        lovverksversjon = "2019-01-01",
                        lovverk = "folketrygdloven",
                        utfall = "VILKAR_BEREGNET",
                        input =
                            mapOf(
                                "sattÅrligInntektPerArbeidsgiver" to
                                    listOf(
                                        mapOf(organisasjonsnummer1 to 500000.0),
                                        mapOf(organisasjonsnummer2 to 300000.0),
                                    ),
                            ),
                        output =
                            mapOf(
                                "grunnlagForSykepengegrunnlag" to 800000.0,
                            ),
                        sporing =
                            mapOf(
                                "vedtaksperiode" to listOf(vedtaksperiodeId.value.toString()),
                                "organisasjonsnummer" to listOf(organisasjonsnummer1, organisasjonsnummer2),
                                "saksbehandler" to listOf(saksbehandler.epost),
                            ),
                        tidsstempel = LocalDateTime.now(), // blir ikke asserted siden den er auto-generert
                        kilde = "spesialist",
                    ),
                "0.0.0",
            ),
        )
    }

    @Test
    fun `Not found hvis personPseudoId ikke finnes`() {
        // given

        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${UUID.randomUUID()}/sykefravaerstilfeller/2023-01-01/sykepengegrunnlag""",
                body =
                    """
                    {
                      "årsak": "En årsak",
                      "sykepengegrunnlagstype": {
                        "discriminatorType": "ApiSkjønnsfastsatt",
                        "skjønnsfastsettelsestype": "OMREGNET_ÅRSINNTEKT",
                        "lovverksreferanse": {
                          "paragraf": "8-30",
                          "ledd": "3",
                          "bokstav": null,
                          "lovverk": "folketrygdloven",
                          "lovverksversjon": "2019-01-01"
                        },
                        "skjønnsfastsatteInntekter": [
                          {
                            "organisasjonsnummer": "${lagOrganisasjonsnummer()}",
                            "årlig": 500000,
                            "fraÅrlig": 400000
                          },
                          {
                            "organisasjonsnummer": "${lagOrganisasjonsnummer()}",
                            "årlig": 300000,
                            "fraÅrlig": 200000
                          }
                        ]
                      },
                      "begrunnelseMal": "En begrunnelse-mal",
                      "begrunnelseFritekst": "En begrunnelse-fritekst",
                      "begrunnelseKonklusjon": "En begrunnelse-konklusjon",
                      "intierendeVedtaksperiodeId": "${UUID.randomUUID()}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "PersonPseudoId har utløpt (eller aldri eksistert)",
              "code": "PERSON_PSEUDO_ID_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `Forbidden hvis saksbehandler ikke har tilgang`() {
        // given
        val person =
            lagPerson().also {
                it.oppdaterEgenAnsattStatus(true, Instant.now())
                personRepository.lagre(it)
            }
        val personPseudoId =
            integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
                .nyPersonPseudoId(person.id)

        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${personPseudoId.value}/sykefravaerstilfeller/2023-01-01/sykepengegrunnlag""",
                brukerroller = emptySet(),
                body =
                    """
                    {
                      "årsak": "En årsak",
                      "sykepengegrunnlagstype": {
                        "discriminatorType": "ApiSkjønnsfastsatt",
                        "skjønnsfastsettelsestype": "OMREGNET_ÅRSINNTEKT",
                        "lovverksreferanse": {
                          "paragraf": "8-30",
                          "ledd": "3",
                          "bokstav": null,
                          "lovverk": "folketrygdloven",
                          "lovverksversjon": "2019-01-01"
                        },
                        "skjønnsfastsatteInntekter": [
                          {
                            "organisasjonsnummer": "${lagOrganisasjonsnummer()}",
                            "årlig": 500000,
                            "fraÅrlig": 400000
                          },
                          {
                            "organisasjonsnummer": "${lagOrganisasjonsnummer()}",
                            "årlig": 300000,
                            "fraÅrlig": 200000
                          }
                        ]
                      },
                      "begrunnelseMal": "En begrunnelse-mal",
                      "begrunnelseFritekst": "En begrunnelse-fritekst",
                      "begrunnelseKonklusjon": "En begrunnelse-konklusjon",
                      "intierendeVedtaksperiodeId": "${UUID.randomUUID()}"
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 403,
              "title": "Mangler tilgang til person",
              "code": "MANGLER_TILGANG_TIL_PERSON" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
