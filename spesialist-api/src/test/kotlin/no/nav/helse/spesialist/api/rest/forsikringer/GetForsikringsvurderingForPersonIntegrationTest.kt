package no.nav.helse.spesialist.api.rest.forsikringer

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.IndividuellForsikring
import no.nav.helse.spesialist.application.KollektivForsikring
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetForsikringsvurderingForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `Returnerer forsikring hvis den finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns
            Forsikringsvurdering(
                identitetsnummer = person.id,
                samletDekning =
                    Forsikringsvurdering.Dekning(
                        grad = 100,
                        fraDag = 17,
                    ),
                kollektivForsikring =
                    KollektivForsikring(
                        navn = "100 % fra 17. dag (Kollektiv)",
                        dekningFolketrygdlovenreferanse =
                            Folketrygdlovenreferanse(
                                kapittel = 8,
                                paragrafIKapittel = 34,
                                ledd = 1,
                                bokstav = null,
                            ),
                        kollektivFolketrygdlovenreferanse =
                            Folketrygdlovenreferanse(
                                kapittel = 8,
                                paragrafIKapittel = 39,
                                ledd = null,
                                bokstav = null,
                            ),
                    ),
                individuelleForsikringer =
                    listOf(
                        IndividuellForsikring(
                            navn = "80 % fra 1. dag (Individuell)",
                            dekningFolketrygdlovenreferanse =
                                Folketrygdlovenreferanse(
                                    kapittel = 8,
                                    paragrafIKapittel = 36,
                                    ledd = 1,
                                    bokstav = 'a',
                                ),
                            virkningsdato = LocalDate.of(2018, 1, 1),
                            opphørsdato = LocalDate.of(2019, 12, 31),
                            konklusjon =
                                IndividuellForsikring.Konklusjon(
                                    forklaring = "Forsikringen opphørte før skjæringstidspunktet",
                                    folketrygdlovenreferanse =
                                        Folketrygdlovenreferanse(
                                            kapittel = 8,
                                            paragrafIKapittel = 37,
                                            ledd = null,
                                            bokstav = null,
                                        ),
                                ),
                            lagtTilGrunn = false,
                        ),
                    ),
                vurdertTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
            )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
               "samletDekning" : {
                  "grad" : 100,
                  "fraDag" : 17
               },
               "kollektivForsikring" : {
                  "navn" : "100 % fra 17. dag (Kollektiv)",
                  "dekningFolketrygdlovenreferanse" : {
                     "kapittel" : 8,
                     "paragrafIKapittel" : 34,
                     "ledd" : 1,
                     "bokstav" : null
                  },
                  "kollektivFolketrygdlovenreferanse" : {
                     "kapittel" : 8,
                     "paragrafIKapittel" : 39,
                     "ledd" : null,
                     "bokstav" : null
                  }
               },
               "individuelleForsikringer" : [
                  {
                     "navn" : "80 % fra 1. dag (Individuell)",
                     "dekningFolketrygdlovenreferanse" : {
                        "kapittel" : 8,
                        "paragrafIKapittel" : 36,
                        "ledd" : 1,
                        "bokstav" : "a"
                     },
                     "virkningsdato" : "2018-01-01",
                     "opphørsdato" : "2019-12-31",
                     "konklusjon" : {
                        "forklaring" : "Forsikringen opphørte før skjæringstidspunktet",
                        "folketrygdlovenreferanse" : {
                           "kapittel" : 8,
                           "paragrafIKapittel" : 37,
                           "ledd" : null,
                           "bokstav" : null
                        }
                     },
                     "lagtTilGrunn" : false
                  }
               ],
               "vurdertTidspunkt" : "2020-02-01T09:30:00Z"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `Returnerer ikke forsikring hvis den ikke finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns
            Forsikringsvurdering(
                identitetsnummer = person.id,
                samletDekning = null,
                kollektivForsikring = null,
                individuelleForsikringer = emptyList(),
                vurdertTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
            )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
              "samletDekning" : null,
              "kollektivForsikring" : null,
              "individuelleForsikringer" : [],
              "vurdertTidspunkt" : "2020-02-01T09:30:00Z"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis personen ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title" : "PersonPseudoId har utløpt (eller aldri eksistert)",
              "code" : "PERSON_PSEUDO_ID_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis forsikringsvurderingen ikke finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns null

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title" : "Forsikringsvurderingen ble ikke funnet",
              "code" : "FORSIKRINGSVURDERING_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis forsikringsvurderingen ikke har samme identitetsnummer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns
            Forsikringsvurdering(
                identitetsnummer = lagIdentitetsnummer(),
                samletDekning =
                    Forsikringsvurdering.Dekning(
                        grad = 100,
                        fraDag = 17,
                    ),
                kollektivForsikring = null,
                individuelleForsikringer = emptyList(),
                vurdertTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
            )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title" : "Forsikringsvurderingen ble ikke funnet",
              "code" : "FORSIKRINGSVURDERING_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
