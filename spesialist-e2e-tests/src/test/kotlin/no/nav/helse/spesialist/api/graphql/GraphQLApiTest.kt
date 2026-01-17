package no.nav.helse.spesialist.api.graphql

import com.github.navikt.tbd_libs.jackson.asYearMonth
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.mar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class GraphQLApiTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter refusjonsopplysninger`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettVedtaksperiode(opprettPerson())
        val pseudoId = sessionFactory.transactionalSessionScope {
            it.personPseudoIdDao.nyPersonPseudoId(Identitetsnummer.fraString(FØDSELSNUMMER))
        }.value
        opprettAvviksvurdering(vilkårsgrunnlagId = vilkårsgrunnlagId)

        val body = runQuery(
            """
            {
                person(personPseudoId:"$pseudoId") {
                    vilkarsgrunnlagV2 {
                        ... on VilkarsgrunnlagSpleisV2 {
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
            body["data"]["person"]["vilkarsgrunnlagV2"].first()["arbeidsgiverrefusjoner"].first()
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
        opprettVedtaksperiode(opprettPerson())
        val pseudoId = sessionFactory.transactionalSessionScope {
            it.personPseudoIdDao.nyPersonPseudoId(Identitetsnummer.fraString(FØDSELSNUMMER))
        }.value

        val body = runQuery(
            """
            {
                person(personPseudoId:"$pseudoId") {
                    vilkarsgrunnlagV2 {
                        ... on VilkarsgrunnlagSpleisV2 {
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
                        avviksvurdering {
                            avviksprosent
                            sammenligningsgrunnlag
                            beregningsgrunnlag
                        }
                    }
                }
                }
            }
        """
        )

        val vilkårsgrunnlag = body["data"]["person"]["vilkarsgrunnlagV2"].first()
        assertEquals("10000", vilkårsgrunnlag["avviksvurdering"]["beregningsgrunnlag"].asText())
        assertEquals("10000", vilkårsgrunnlag["avviksvurdering"]["sammenligningsgrunnlag"].asText())
        assertEquals("26", vilkårsgrunnlag["avviksvurdering"]["avviksprosent"].asText())
        assertEquals(2, vilkårsgrunnlag["inntekter"].size())

        val sammenligningsgrunnlag1 = vilkårsgrunnlag["inntekter"][0]["sammenligningsgrunnlag"]
        assertEquals(4000.0, sammenligningsgrunnlag1["belop"].asDouble())
        assertEquals(2, sammenligningsgrunnlag1["inntektFraAOrdningen"].size())

        val inntekt1_1 = sammenligningsgrunnlag1["inntektFraAOrdningen"][0]
        assertEquals(YearMonth.from(1 jan 2018), inntekt1_1["maned"].asYearMonth())
        assertEquals(2000.0, inntekt1_1["sum"].asDouble())
        val inntekt1_2 = sammenligningsgrunnlag1["inntektFraAOrdningen"][1]
        assertEquals(YearMonth.from(1 feb 2018), inntekt1_2["maned"].asYearMonth())
        assertEquals(2000.0, inntekt1_2["sum"].asDouble())

        val sammenligningsgrunnlag2 = vilkårsgrunnlag["inntekter"][1]["sammenligningsgrunnlag"]
        assertEquals(6000.0, sammenligningsgrunnlag2["belop"].asDouble())
        assertEquals(4, sammenligningsgrunnlag2["inntektFraAOrdningen"].size())

        val inntekt2_1 = sammenligningsgrunnlag2["inntektFraAOrdningen"][0]
        assertEquals(YearMonth.from(1 jan 2018), inntekt2_1["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_1["sum"].asDouble())
        val inntekt2_2 = sammenligningsgrunnlag2["inntektFraAOrdningen"][1]
        assertEquals(YearMonth.from(1 feb 2018), inntekt2_2["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_2["sum"].asDouble())
        val inntekt2_3 = sammenligningsgrunnlag2["inntektFraAOrdningen"][2]
        assertEquals(YearMonth.from(1 mar 2018), inntekt2_3["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_3["sum"].asDouble())
        val inntekt2_4 = sammenligningsgrunnlag2["inntektFraAOrdningen"][3]
        assertEquals(YearMonth.from(1 apr 2018), inntekt2_4["maned"].asYearMonth())
        assertEquals(1500.0, inntekt2_4["sum"].asDouble())
    }

    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        mockSnapshot(vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettAvviksvurdering(avviksprosent = 0.0, vilkårsgrunnlagId = vilkårsgrunnlagId)
        opprettVedtaksperiode(opprettPerson())
        val pseudoId = sessionFactory.transactionalSessionScope {
            it.personPseudoIdDao.nyPersonPseudoId(Identitetsnummer.fraString(FØDSELSNUMMER))
        }.value

        val body = runQuery(
            """
            {
                person(personPseudoId:"$pseudoId") {
                    vilkarsgrunnlagV2 {
                        skjaeringstidspunkt
                         ... on VilkarsgrunnlagSpleisV2 {
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
            body["data"]["person"]["vilkarsgrunnlagV2"].first()["sykepengegrunnlagsgrense"]

        assertEquals(100_000, sykepengegrunnlagsgrense["grunnbelop"].asInt())
        assertEquals(600_000, sykepengegrunnlagsgrense["grense"].asInt())
        assertEquals("2020-01-01", sykepengegrunnlagsgrense["virkningstidspunkt"].asText())
    }
}
