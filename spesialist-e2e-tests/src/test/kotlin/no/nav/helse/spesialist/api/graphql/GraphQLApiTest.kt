package no.nav.helse.spesialist.api.graphql

import com.github.navikt.tbd_libs.jackson.asYearMonth
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.april
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal open class GraphQLApiTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter refusjonsopplysninger`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettAvviksvurdering(vilkårsgrunnlagId = vilkårsgrunnlagId)

        val body = runQuery(
            """
            {
                person(fnr:"$FNR") {
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
            body["data"]["person"]["vilkarsgrunnlag"].first()["arbeidsgiverrefusjoner"].first()
                .get("refusjonsopplysninger").first()
        assertEquals("2020-01-01", refusjonsopplysning["fom"].asText())
        assertTrue(refusjonsopplysning["tom"].isNull)
        assertEquals(30000.0, refusjonsopplysning["belop"].asDouble())

    }

    @Test
    fun `beriker vilkårsgrunnlag med data fra avviksvurdering`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettAvviksvurdering(avviksprosent = 26.0, vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val body = runQuery(
            """
            {
                person(fnr:"$FNR") {
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
        assertEquals(2, vilkårsgrunnlag["inntekter"].size())

        val sammenligningsgrunnlag1 = vilkårsgrunnlag["inntekter"][0]["sammenligningsgrunnlag"]
        assertEquals(4000.0, sammenligningsgrunnlag1["belop"].asDouble())
        assertEquals(2, sammenligningsgrunnlag1["inntektFraAOrdningen"].size())

        val inntekt1_1 = sammenligningsgrunnlag1["inntektFraAOrdningen"][0]
        assertEquals(YearMonth.from(1.januar), inntekt1_1["maned"].asYearMonth())
        assertEquals(2000.0, inntekt1_1["sum"].asDouble())
        val inntekt1_2 = sammenligningsgrunnlag1["inntektFraAOrdningen"][1]
        assertEquals(YearMonth.from(1.februar), inntekt1_2["maned"].asYearMonth())
        assertEquals(2000.0, inntekt1_2["sum"].asDouble())

        val sammenligningsgrunnlag2 = vilkårsgrunnlag["inntekter"][1]["sammenligningsgrunnlag"]
        assertEquals(6000.0, sammenligningsgrunnlag2["belop"].asDouble())
        assertEquals(4, sammenligningsgrunnlag2["inntektFraAOrdningen"].size())

        val inntekt2_1 = sammenligningsgrunnlag2["inntektFraAOrdningen"][0]
        assertEquals(YearMonth.from(1.januar), inntekt2_1["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_1["sum"].asDouble())
        val inntekt2_2 = sammenligningsgrunnlag2["inntektFraAOrdningen"][1]
        assertEquals(YearMonth.from(1.februar), inntekt2_2["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_2["sum"].asDouble())
        val inntekt2_3 = sammenligningsgrunnlag2["inntektFraAOrdningen"][2]
        assertEquals(YearMonth.from(1.mars), inntekt2_3["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_3["sum"].asDouble())
        val inntekt2_4 = sammenligningsgrunnlag2["inntektFraAOrdningen"][3]
        assertEquals(YearMonth.from(1.april), inntekt2_4["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_4["sum"].asDouble())
    }

    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettAvviksvurdering(avviksprosent = 0.0, vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val body = runQuery(
            """
            {
                person(fnr:"$FNR") {
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
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettAvviksvurdering(avviksprosent = avviksprosent, vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val body = runQuery(
            """
            {
                person(fnr:"$FNR") {
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

    private fun opprettAvviksvurdering(
        fødselsnummer: String = FNR,
        skjæringstidspunkt: LocalDate = 1.januar,
        avviksvurderingId: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        avviksprosent: Double = 25.0
    ) {
        sessionFactory.transactionalSessionScope {
            it.avviksvurderingRepository.lagre(
                Avviksvurdering(
                    unikId = avviksvurderingId,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    opprettet = 1.januar.atStartOfDay(),
                    avviksprosent = avviksprosent,
                    sammenligningsgrunnlag =
                        Sammenligningsgrunnlag(
                            totalbeløp = 10000.0,
                            innrapporterteInntekter =
                                listOf(
                                    InnrapportertInntekt(
                                        arbeidsgiverreferanse = ORGNUMMER,
                                        inntekter =
                                            listOf(
                                                Inntekt(
                                                    årMåned = YearMonth.from(1.januar),
                                                    beløp = 2000.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1.februar),
                                                    beløp = 2000.0,
                                                ),
                                            ),
                                    ),
                                    InnrapportertInntekt(
                                        arbeidsgiverreferanse = "987656789",
                                        inntekter =
                                            listOf(
                                                Inntekt(
                                                    årMåned = YearMonth.from(1.januar),
                                                    beløp = 1500.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1.februar),
                                                    beløp = 1500.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1.mars),
                                                    beløp = 1500.0,
                                                ),
                                                Inntekt(
                                                    årMåned = YearMonth.from(1.april),
                                                    beløp = 1500.0,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    beregningsgrunnlag =
                        Beregningsgrunnlag(
                            totalbeløp = 10000.0,
                            omregnedeÅrsinntekter =
                                listOf(
                                    OmregnetÅrsinntekt(arbeidsgiverreferanse = ORGNUMMER, beløp = 10000.0),
                                ),
                        ),
                )

            )
        }
    }

}
