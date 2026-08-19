package no.nav.helse.spesialist.api.rest.personer.sykefraværstilfeller.arbeidsforhold

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.OverstyrtArbeidsforholdEvent
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.overstyringer.Arbeidsforhold
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtArbeidsforhold
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class PostArbeidsforholdoverstyringBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personRepository = integrationTestFixture.sessionContext.personRepository
    private val totrinnsvurderingRepository = integrationTestFixture.sessionContext.totrinnsvurderingRepository
    private val reservasjonDao = integrationTestFixture.sessionContext.reservasjonDao

    @Test
    fun `happy path`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.personPseudoIdProvider
                .nyPersonPseudoId(person.id)
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val vedtaksperiodeId = lagVedtaksperiodeId()
        val saksbehandler = lagSaksbehandler()
        val skjæringstidspunkt = 1.jan(2018)

        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${personPseudoId.value}/sykefravaerstilfeller/$skjæringstidspunkt/arbeidsforhold""",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "initierendeVedtaksperiodeId": "${vedtaksperiodeId.value}",
                      "overstyrteArbeidsforhold": [
                        {
                          "organisasjonsnummer": "$organisasjonsnummer1",
                          "deaktivert": true,
                          "begrunnelse": "En begrunnelse",
                          "forklaring": "En forklaring",
                          "lovverksreferanse": {
                            "paragraf": "8-15",
                            "ledd": null,
                            "bokstav": null,
                            "lovverk": "folketrygdloven",
                            "lovverksversjon": "2019-01-01"
                          }
                        },
                        {
                          "organisasjonsnummer": "$organisasjonsnummer2",
                          "deaktivert": false,
                          "begrunnelse": "En annen begrunnelse",
                          "forklaring": "En annen forklaring"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.NoContent.value, response.status)

        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
        assertNotNull(totrinnsvurdering)
        assertEquals(1, totrinnsvurdering.overstyringer.size)
        val overstyring = totrinnsvurdering.overstyringer.single()
        assertIs<OverstyrtArbeidsforhold>(overstyring)

        assertEquals(person.id.value, overstyring.fødselsnummer)
        assertEquals(person.aktørId, overstyring.aktørId)
        assertEquals(vedtaksperiodeId.value, overstyring.vedtaksperiodeId)
        assertEquals(skjæringstidspunkt, overstyring.skjæringstidspunkt)
        assertEquals(saksbehandler.id, overstyring.saksbehandlerOid)
        assertEquals(false, overstyring.ferdigstilt)
        assertEquals(
            listOf(
                Arbeidsforhold(
                    organisasjonsnummer = organisasjonsnummer1,
                    deaktivert = true,
                    begrunnelse = "En begrunnelse",
                    forklaring = "En forklaring",
                    lovhjemmel =
                        Lovhjemmel(
                            paragraf = "8-15",
                            ledd = null,
                            bokstav = null,
                            lovverk = "folketrygdloven",
                            lovverksversjon = "2019-01-01",
                        ),
                ),
                Arbeidsforhold(
                    organisasjonsnummer = organisasjonsnummer2,
                    deaktivert = false,
                    begrunnelse = "En annen begrunnelse",
                    forklaring = "En annen forklaring",
                    lovhjemmel = null,
                ),
            ),
            overstyring.overstyrteArbeidsforhold,
        )

        assertEquals(saksbehandler.id, reservasjonDao.hentReservasjonFor(person.id.value)?.reservertTil?.id)

        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                person.id.value,
                OverstyrtArbeidsforholdEvent(
                    eksternHendelseId = overstyring.eksternHendelseId,
                    fødselsnummer = person.id.value,
                    aktørId = person.aktørId,
                    saksbehandlerOid = saksbehandler.id.value,
                    saksbehandlerNavn = saksbehandler.navn,
                    saksbehandlerIdent = saksbehandler.ident.value,
                    saksbehandlerEpost = saksbehandler.epost,
                    skjæringstidspunkt = skjæringstidspunkt,
                    overstyrteArbeidsforhold =
                        listOf(
                            OverstyrtArbeidsforholdEvent.Arbeidsforhold(
                                orgnummer = organisasjonsnummer1,
                                deaktivert = true,
                                begrunnelse = "En begrunnelse",
                                forklaring = "En forklaring",
                            ),
                            OverstyrtArbeidsforholdEvent.Arbeidsforhold(
                                orgnummer = organisasjonsnummer2,
                                deaktivert = false,
                                begrunnelse = "En annen begrunnelse",
                                forklaring = "En annen forklaring",
                            ),
                        ),
                ),
                årsak = "overstyring av arbeidsforhold",
            ),
        )
    }

    @Test
    fun `Bad request hvis lista med arbeidsforhold er tom`() {
        // given
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.personPseudoIdProvider
                .nyPersonPseudoId(person.id)

        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${personPseudoId.value}/sykefravaerstilfeller/2018-01-01/arbeidsforhold""",
                body =
                    """
                    {
                      "initierendeVedtaksperiodeId": "${UUID.randomUUID()}",
                      "overstyrteArbeidsforhold": []
                    }
                    """.trimIndent(),
            )

        // then
        assertEquals(HttpStatusCode.BadRequest.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 400,
              "title": "Mangler arbeidsforhold å overstyre",
              "code": "TOM_LISTE_MED_ARBEIDSFORHOLD"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Not found hvis personPseudoId ikke finnes`() {
        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${UUID.randomUUID()}/sykefravaerstilfeller/2018-01-01/arbeidsforhold""",
                body =
                    """
                    {
                      "initierendeVedtaksperiodeId": "${UUID.randomUUID()}",
                      "overstyrteArbeidsforhold": [
                        {
                          "organisasjonsnummer": "${lagOrganisasjonsnummer()}",
                          "deaktivert": true,
                          "begrunnelse": "En begrunnelse",
                          "forklaring": "En forklaring"
                        }
                      ]
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
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Forbidden hvis saksbehandler ikke har tilgang`() {
        // given
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(
                TilgangSomMangler.StrengtFortroligAdresse,
            )
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId =
            integrationTestFixture.personPseudoIdProvider
                .nyPersonPseudoId(person.id)

        // when
        val response =
            integrationTestFixture.post(
                url = """/api/personer/${personPseudoId.value}/sykefravaerstilfeller/2018-01-01/arbeidsforhold""",
                brukerroller = emptySet(),
                body =
                    """
                    {
                      "initierendeVedtaksperiodeId": "${UUID.randomUUID()}",
                      "overstyrteArbeidsforhold": [
                        {
                          "organisasjonsnummer": "${lagOrganisasjonsnummer()}",
                          "deaktivert": true,
                          "begrunnelse": "En begrunnelse",
                          "forklaring": "En forklaring"
                        }
                      ]
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
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
