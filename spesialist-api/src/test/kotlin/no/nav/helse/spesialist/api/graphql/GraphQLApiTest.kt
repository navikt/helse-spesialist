package no.nav.helse.spesialist.api.graphql

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

        val query = queryize("""{ person(fnr:"$FØDSELSNUMMER") { fodselsnummer } }""")
        val body = runQuery(query)

        assertEquals(FØDSELSNUMMER, body["data"]["person"]["fodselsnummer"].asText())
    }

    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        opprettVedtaksperiode()

        val query = queryize(
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

        val body = runQuery(query)
        val sykepengegrunnlagsgrense = body["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]

        assertEquals(100_000, sykepengegrunnlagsgrense["grunnbelop"].asInt())
        assertEquals(600_000, sykepengegrunnlagsgrense["grense"].asInt())
        assertEquals("2020-01-01", sykepengegrunnlagsgrense["virkningstidspunkt"].asText())
    }

    @Test
    fun `Infinity avviksprosent gir error`() = ugyldigAvvikprosent(Double.POSITIVE_INFINITY)

    @Test
    fun `Nan avviksprosent gir error`() = ugyldigAvvikprosent(Double.NaN)

    private fun ugyldigAvvikprosent(avviksprosent: Double) {
        mockSnapshot(avviksprosent = avviksprosent)
        opprettVedtaksperiode()

        val query = queryize("""
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

        val body = runQuery(query)
        val grunnlag = body["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"]
        val forventetError = objectMapper.readTree(
            """
            {
                "message": "Can't serialize value (/person/vilkarsgrunnlaghistorikk[0]/grunnlag[0]/avviksprosent) : Expected type 'Float' but was 'Double'.",
                "path": ["person", "vilkarsgrunnlaghistorikk", 0, "grunnlag", 0, "avviksprosent"]
            }
        """
        )

        assertEquals(forventetError, body["errors"].first())
        assertNotNull(grunnlag)
        assertNull(grunnlag["avviksprosent"])
    }

}
