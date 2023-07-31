package no.nav.helse.spesialist.api.graphql

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class GraphQLApiTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter refusjonsopplysninger`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(
            """
            {
                person(fnr:"$FØDSELSNUMMER") {
                    vilkarsgrunnlag {
                        ... on VilkarsgrunnlagSpleis {
                          arbeidsgiverrefusjoner {
                            arbeidsgiver
                            refusjonsopplysninger {
                                fom,
                                tom,
                                belop,
                                meldingsreferanseId
                            }
                        }
                    }
                }
                }
            }
        """
        )
        val refusjonsopplysning =
            body["data"]["person"]["vilkarsgrunnlag"].first()["arbeidsgiverrefusjoner"].first().get("refusjonsopplysninger").first()
        assertEquals("2020-01-01", refusjonsopplysning["fom"].asText())
        assertTrue(refusjonsopplysning["tom"].isNull)
        assertEquals(30000.0, refusjonsopplysning["belop"].asDouble())

    }
    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        mockSnapshot()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(
            """
            {
                person(fnr:"$FØDSELSNUMMER") {
                    vilkarsgrunnlag {
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
        """
        )
        val sykepengegrunnlagsgrense =
            body["data"]["person"]["vilkarsgrunnlag"].first()["sykepengegrunnlagsgrense"]

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
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(
            """
            {
                person(fnr:"$FØDSELSNUMMER") {
                    vilkarsgrunnlag {
                        skjaeringstidspunkt
                        ... on VilkarsgrunnlagSpleis {
                            avviksprosent
                        }
                    }
                }
            }
        """
        )
        val grunnlag = body["data"]["person"]["vilkarsgrunnlag"].first()
        val forventetError = objectMapper.readTree(
            """
            {
                "message": "Can't serialize value (/person/vilkarsgrunnlag[0]/avviksprosent) : Expected a value that can be converted to type 'Float' but it was a 'Double'",
                "path": ["person", "vilkarsgrunnlag", 0, "avviksprosent"]
            }
        """
        )

        assertEquals(forventetError, body["errors"].first())
        assertNotNull(grunnlag)
        assertTrue(grunnlag["avviksprosent"].isNull)
    }

}
