package no.nav.helse.spesialist.api.rest.forsikringer

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.Ekskluderingsbegrunnelse
import no.nav.helse.spesialist.application.Ekskluderingsårsak
import no.nav.helse.spesialist.application.EkskludertForsikring
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikring
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testfixtures.lagForsikring
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
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

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
                harForsikring = true,
                dekning =
                    Forsikringsvurdering.Dekning(
                        grad = 100,
                        fraDag = 17,
                    ),
                ekskluderteForsikringer =
                    listOf(
                        EkskludertForsikring(
                            virkningsdato = LocalDate.of(2018, 1, 1),
                            opphørsdato = LocalDate.of(2019, 12, 31),
                            dekningsgrad = 80,
                            dekningIVentetid = false,
                            navn = "80 % fra dag 1",
                            folketrygdlovenreferanse =
                                Folketrygdlovenreferanse(
                                    kapittel = 8,
                                    paragrafIKapittel = 36,
                                    ledd = 1,
                                    bokstav = 'a',
                                ),
                            ekskluderingsårsak = Ekskluderingsårsak.OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT,
                            ekskluderingsbegrunnelse =
                                Ekskluderingsbegrunnelse(
                                    forklaring = "Forsikringen var opphørt på skjæringstidspunktet",
                                    folketrygdlovenreferanse =
                                        Folketrygdlovenreferanse(
                                            kapittel = 8,
                                            paragrafIKapittel = 37,
                                            ledd = null,
                                            bokstav = null,
                                        ),
                                ),
                        ),
                    ),
                gjeldendeForsikring =
                    Forsikring(
                        virkningsdato = LocalDate.of(2020, 1, 1),
                        opphørsdato = null,
                        dekningsgrad = 100,
                        dekningIVentetid = false,
                        navn = "100 % fra dag 17",
                        folketrygdlovenreferanse =
                            Folketrygdlovenreferanse(
                                kapittel = 8,
                                paragrafIKapittel = 36,
                                ledd = 1,
                                bokstav = 'b',
                            ),
                    ),
                dataHentetTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
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
               "eksisterer" : true,
               "forsikringInnhold" : {
                  "gjelderFraDag" : 17,
                  "dekningsgrad" : 100
               },
               "ekskluderteForsikringer" : [
                  {
                     "virkningsdato" : "2018-01-01",
                     "opphørsdato" : "2019-12-31",
                     "dekningsgrad" : 80,
                     "dekningIVentetid" : false,
                     "navn" : "80 % fra dag 1",
                     "folketrygdlovenreferanse" : {
                        "kapittel" : 8,
                        "paragrafIKapittel" : 36,
                        "ledd" : 1,
                        "bokstav" : "a"
                     },
                     "ekskluderingsårsak" : "OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT",
                     "ekskluderingsbegrunnelse" : {
                        "forklaring" : "Forsikringen var opphørt på skjæringstidspunktet",
                        "folketrygdlovenreferanse" : {
                           "kapittel" : 8,
                           "paragrafIKapittel" : 37,
                           "ledd" : null,
                           "bokstav" : null
                        }
                     }
                  }
               ],
               "gjeldendeForsikring" : {
                  "virkningsdato" : "2020-01-01",
                  "opphørsdato" : null,
                  "dekningsgrad" : 100,
                  "dekningIVentetid" : false,
                  "navn" : "100 % fra dag 17",
                  "folketrygdlovenreferanse" : {
                     "kapittel" : 8,
                     "paragrafIKapittel" : 36,
                     "ledd" : 1,
                     "bokstav" : "b"
                  }
               },
               "dataHentetTidspunkt" : "2020-02-01T09:30:00Z"
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
                harForsikring = false,
                dekning = null,
                ekskluderteForsikringer = emptyList(),
                gjeldendeForsikring = null,
                dataHentetTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
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
              "eksisterer" : false,
              "forsikringInnhold" : null,
              "ekskluderteForsikringer" : [],
              "gjeldendeForsikring" : null,
              "dataHentetTidspunkt" : "2020-02-01T09:30:00Z"
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
                harForsikring = true,
                dekning =
                    Forsikringsvurdering.Dekning(
                        grad = 100,
                        fraDag = 17,
                    ),
                ekskluderteForsikringer = emptyList(),
                gjeldendeForsikring =
                    lagForsikring(
                        virkningsdato = LocalDate.of(2020, 1, 1),
                        opphørsdato = null,
                        dekningsgrad = 100,
                        dekningIVentetid = false,
                    ),
                dataHentetTidspunkt = Instant.parse("2020-02-01T09:30:00Z"),
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
