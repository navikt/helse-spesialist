package no.nav.helse.spesialist.api.graphql

import java.time.YearMonth
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.januar
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
    fun `beriker vilkårsgrunnlag med data fra avviksvurdering`() {
        mockSnapshot(avviksprosent = 26.0)
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(
            """
            {
                person(fnr:"$FØDSELSNUMMER") {
                    vilkarsgrunnlag {
                        ... on VilkarsgrunnlagSpleis {
                          inntekter {
                            arbeidsgiver
                            sammenligningsgrunnlag {
                                inntektFraAOrdningen {
                                    maned
                                    sum
                                }
                                belop
                            }
                        }
                        avviksprosent
                        sammenligningsgrunnlag
                        omregnetArsinntekt
                    }
                }
                }
            }
        """
        )

        val vilkårsgrunnlag = body["data"]["person"]["vilkarsgrunnlag"].first()
        assertEquals(10000.0, vilkårsgrunnlag["omregnetArsinntekt"].asDouble())
        assertEquals(10000.0, vilkårsgrunnlag["sammenligningsgrunnlag"].asDouble())
        assertEquals(26.0, vilkårsgrunnlag["avviksprosent"].asDouble())
        assertEquals(1, vilkårsgrunnlag["inntekter"].size())

        val sammenligningsgrunnlag = vilkårsgrunnlag["inntekter"].first()["sammenligningsgrunnlag"]
        assertEquals(10000.0, sammenligningsgrunnlag["belop"].asDouble())
        assertEquals(1, sammenligningsgrunnlag["inntektFraAOrdningen"].size())

        val inntekt = sammenligningsgrunnlag["inntektFraAOrdningen"].first()
        assertEquals(YearMonth.from(1.januar), inntekt["maned"].asYearMonth())
        assertEquals(10000.0, inntekt["sum"].asDouble())
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
