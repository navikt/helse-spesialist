package no.nav.helse.spesialist.api.graphql

import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class GraphQLApiTest : AbstractGraphQLApiTest() {

    @BeforeAll
    fun setup() {
        setupGraphQLServer()
    }

    @Test
    fun `henter fødselsnummer`() {
        opprettVedtaksperiode()
        val queryString = queryize(
            """
            {
                person(fnr:"$FØDSELSNUMMER") {
                    fodselsnummer
                }
            }
        """
        )

        val body = runBlocking {
            val response = client.preparePost("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(UUID.randomUUID())
                setBody(mapOf("query" to queryString))
            }.execute()
            response.body<String>()
        }

        assertEquals(FØDSELSNUMMER, objectMapper.readTree(body)["data"]["person"]["fodselsnummer"].asText())
    }

    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        opprettVedtaksperiode()
        val queryString = queryize(
            """
            {
                person(fnr:"$FØDSELSNUMMER") {
                    vilkarsgrunnlaghistorikk {
                        id,
                        grunnlag {
                            skjaeringstidspunkt
                             ... on VilkarsgrunnlagSpleis {
                                sykepengegrunnlagsgrense {
                                    grunnbelop,
                                    grense,
                                    virkningstidspunkt
                                }
                             }
                        }
                    }
                }
            }
        """
        )


        val body = runBlocking {
            val response = client.preparePost("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(UUID.randomUUID())
                setBody(mapOf("query" to queryString))
            }.execute()
            response.body<String>()
        }

        assertEquals(
            100_000,
            objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["grunnbelop"].asInt()
        )
        assertEquals(
            600_000,
            objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["grense"].asInt()
        )
        assertEquals(
            "2020-01-01",
            objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["virkningstidspunkt"].asText()
        )
    }

    @Test
    fun `Infinity avviksprosent gir error`() = ugyldigAvvikprosent(Double.POSITIVE_INFINITY)

    @Test
    fun `Nan avviksprosent gir error`() = ugyldigAvvikprosent(Double.NaN)

    private fun ugyldigAvvikprosent(avviksprosent: Double) {
        mockSnapshot(avviksprosent = avviksprosent)
        opprettVedtaksperiode()

        val queryString = queryize("""
            {
                person(fnr:"$FØDSELSNUMMER") {
                    vilkarsgrunnlaghistorikk {
                        id,
                        grunnlag {
                            skjaeringstidspunkt
                             ... on VilkarsgrunnlagSpleis {
                                avviksprosent
                             }
                        }
                    }
                }
            }
        """)

        val body = runBlocking {
            val response = client.preparePost("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(UUID.randomUUID())
                setBody(mapOf("query" to queryString))
            }.execute()
            response.body<String>()
        }

        val errors = objectMapper.readTree(body)["errors"]
        val forventet = objectMapper.readTree(
            """
            {
                "message": "Can't serialize value (/person/vilkarsgrunnlaghistorikk[0]/grunnlag[0]/avviksprosent) : Expected type 'Float' but was 'Double'.",
                "path": ["person", "vilkarsgrunnlaghistorikk", 0, "grunnlag", 0, "avviksprosent"]
            }
        """
        )
        assertEquals(forventet, errors.first())
        val grunnlag = objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"]
        assertNotNull(grunnlag)
        assertNull(grunnlag["avviksprosent"])
    }

}
