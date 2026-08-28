package no.nav.helse.spesialist.e2etests.tests

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.rest.ApiLovhjemmel
import no.nav.helse.spesialist.api.rest.ApiOverstyrArbeidsforholdRequest
import no.nav.helse.spesialist.api.rest.ApiOverstyrInntektOgRefusjonRequest
import no.nav.helse.spesialist.api.rest.ApiOverstyrTidslinjeRequest
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import no.nav.helse.spesialist.e2etests.E2ETestApplikasjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OverstyringE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler overstyrer sykdomstidslinje`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerTidslinje(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                dager =
                    listOf(
                        ApiOverstyrTidslinjeRequest.Dag(
                            dato = vedtaksperiode.fom.plusDays(19),
                            type = "Feriedag",
                            fraType = "Sykedag",
                            grad = null,
                            fraGrad = 100,
                            lovhjemmel = null,
                        ),
                    ),
            )
        }

        assertAntallOverstyringTidslinje(1)

        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertOppgavestatuserKronoligisk("Invalidert", "AvventerSaksbehandler")
    }

    @Test
    fun `saksbehandler overstyrer sykdomstidslinje med referanse til lovhjemmel`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerTidslinje(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                dager =
                    listOf(
                        ApiOverstyrTidslinjeRequest.Dag(
                            dato = vedtaksperiode.fom.plusDays(19),
                            type = "Feriedag",
                            fraType = "Sykedag",
                            grad = null,
                            fraGrad = 100,
                            lovhjemmel =
                                ApiLovhjemmel(
                                    paragraf = "EN PARAGRAF",
                                    ledd = "ET LEDD",
                                    bokstav = "EN BOKSTAV",
                                    lovverk = "folketrygdloven",
                                    lovverksversjon = "1970-01-01",
                                ),
                        ),
                    ),
            )
        }

        val subsumsjon = sisteSendteMeldingMedEventName("subsumsjon").path("subsumsjon")
        assertNotNull(subsumsjon["sporing"]["overstyrtidslinje"])
    }

    @Test
    fun `saksbehandler overstyrer inntekt og refusjon`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerInntektOgRefusjon(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
                arbeidsgivere =
                    listOf(
                        ApiOverstyrInntektOgRefusjonRequest.Arbeidsgiver(
                            organisasjonsnummer = organisasjonsnummer(),
                            månedligInntekt = 25000.0,
                            fraMånedligInntekt = 25001.0,
                            refusjonsopplysninger = null,
                            fraRefusjonsopplysninger = null,
                            begrunnelse = "begrunnelse",
                            forklaring = "testbortforklaring",
                            lovhjemmel = ApiLovhjemmel("8-28", "LEDD_1", "BOKSTAV_A", "folketrygdloven", "1970-01-01"),
                            fom = null,
                            tom = null,
                        ),
                    ),
            )
        }

        assertAntallOverstyringInntektOgRefusjon(1)

        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertOppgavestatuserKronoligisk("Invalidert", "AvventerSaksbehandler")
        assertOppgaveTildeltSaksbehandlerEvent()
    }

    @Test
    fun `saksbehandler overstyrer arbeidsforhold`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerArbeidsforhold(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
                overstyrteArbeidsforhold =
                    listOf(
                        ApiOverstyrArbeidsforholdRequest.Arbeidsforhold(
                            organisasjonsnummer = organisasjonsnummer(),
                            deaktivert = true,
                            begrunnelse = "begrunnelse",
                            forklaring = "forklaring",
                            lovhjemmel = ApiLovhjemmel("8-15", null, null, "folketrygdloven", "1998-12-18"),
                        ),
                    ),
            )
        }

        assertAntallOverstyringArbeidsforhold(1)

        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertOppgavestatuserKronoligisk("Invalidert", "AvventerSaksbehandler")
        assertOppgaveTildeltSaksbehandlerEvent()
    }

    @Test
    fun `legger ved overstyringer i speil snapshot`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        val vedtaksperiode = søknadOgGodkjenningbehovKommerInn()
        val dagensDato = vedtaksperiode.fom.plusDays(19)

        medPersonISpeil {
            saksbehandlerOverstyrerTidslinje(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                dager =
                    listOf(
                        ApiOverstyrTidslinjeRequest.Dag(
                            dato = dagensDato,
                            type = "Feriedag",
                            fraType = "Sykedag",
                            grad = null,
                            fraGrad = 100,
                            lovhjemmel = null,
                        ),
                    ),
                begrunnelse = "begrunnelse for tidslinje",
            )
        }
        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        medPersonISpeil {
            saksbehandlerOverstyrerInntektOgRefusjon(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
                arbeidsgivere =
                    listOf(
                        ApiOverstyrInntektOgRefusjonRequest.Arbeidsgiver(
                            organisasjonsnummer = organisasjonsnummer(),
                            månedligInntekt = 25000.0,
                            fraMånedligInntekt = 25001.0,
                            refusjonsopplysninger = null,
                            fraRefusjonsopplysninger = null,
                            begrunnelse = "begrunnelse for inntekt",
                            forklaring = "forklaring for inntekt",
                            lovhjemmel = null,
                            fom = null,
                            tom = null,
                        ),
                    ),
            )
        }
        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        medPersonISpeil {
            saksbehandlerOverstyrerArbeidsforhold(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
                overstyrteArbeidsforhold =
                    listOf(
                        ApiOverstyrArbeidsforholdRequest.Arbeidsforhold(
                            organisasjonsnummer = organisasjonsnummer(),
                            deaktivert = true,
                            begrunnelse = "begrunnelse for arbeidsforhold",
                            forklaring = "forklaring for arbeidsforhold",
                            lovhjemmel = null,
                        ),
                    ),
            )

            val overstyringer = person["arbeidsgivere"][0]["overstyringer"].toList()
            assertEquals(3, overstyringer.size)

            val tidslinjeOverstyring = overstyringer[0]
            assertEquals("Dagoverstyring", tidslinjeOverstyring["__typename"].asString())
            assertEquals(vedtaksperiode.vedtaksperiodeId.toString(), tidslinjeOverstyring["vedtaksperiodeId"].asString())
            assertEquals("begrunnelse for tidslinje", tidslinjeOverstyring["begrunnelse"].asString())
            assertEquals(saksbehandlerIdent().value, tidslinjeOverstyring["saksbehandler"]["ident"].asString())
            assertEquals(false, tidslinjeOverstyring["ferdigstilt"].asBoolean())
            val overstyrtDag = tidslinjeOverstyring["dager"].single()
            assertEquals(dagensDato.toString(), overstyrtDag["dato"].asString())
            assertEquals("Feriedag", overstyrtDag["type"].asString())
            assertEquals("Sykedag", overstyrtDag["fraType"].asString())
            assertTrue(overstyrtDag["grad"].isNull)
            assertEquals(100, overstyrtDag["fraGrad"].asInt())

            val inntektOverstyring = overstyringer[1]
            assertEquals("Inntektoverstyring", inntektOverstyring["__typename"].asString())
            assertEquals(vedtaksperiode.vedtaksperiodeId.toString(), inntektOverstyring["vedtaksperiodeId"].asString())
            assertEquals(saksbehandlerIdent().value, inntektOverstyring["saksbehandler"]["ident"].asString())
            assertEquals(false, inntektOverstyring["ferdigstilt"].asBoolean())
            val overstyrtInntekt = inntektOverstyring["inntekt"]
            assertEquals(vedtaksperiode.skjæringstidspunkt.toString(), overstyrtInntekt["skjaeringstidspunkt"].asString())
            assertEquals("begrunnelse for inntekt", overstyrtInntekt["begrunnelse"].asString())
            assertEquals("forklaring for inntekt", overstyrtInntekt["forklaring"].asString())
            assertEquals(25000.0, overstyrtInntekt["manedligInntekt"].asDouble())
            assertEquals(25001.0, overstyrtInntekt["fraManedligInntekt"].asDouble())

            val arbeidsforholdOverstyring = overstyringer[2]
            assertEquals("Arbeidsforholdoverstyring", arbeidsforholdOverstyring["__typename"].asString())
            assertEquals(vedtaksperiode.vedtaksperiodeId.toString(), arbeidsforholdOverstyring["vedtaksperiodeId"].asString())
            assertEquals(saksbehandlerIdent().value, arbeidsforholdOverstyring["saksbehandler"]["ident"].asString())
            assertEquals(false, arbeidsforholdOverstyring["ferdigstilt"].asBoolean())
            assertEquals(true, arbeidsforholdOverstyring["deaktivert"].asBoolean())
            assertEquals(vedtaksperiode.skjæringstidspunkt.toString(), arbeidsforholdOverstyring["skjaeringstidspunkt"].asString())
            assertEquals("begrunnelse for arbeidsforhold", arbeidsforholdOverstyring["begrunnelse"].asString())
            assertEquals("forklaring for arbeidsforhold", arbeidsforholdOverstyring["forklaring"].asString())
        }
    }

    private fun assertAntallOverstyringTidslinje(forventetAntall: Int) = assertAntallOverstyringer("overstyring_tidslinje", forventetAntall)

    private fun assertAntallOverstyringInntektOgRefusjon(forventetAntall: Int) = assertAntallOverstyringer("overstyring_inntekt", forventetAntall)

    private fun assertAntallOverstyringArbeidsforhold(forventetAntall: Int) = assertAntallOverstyringer("overstyring_arbeidsforhold", forventetAntall)

    private fun assertAntallOverstyringer(
        tabell: String,
        forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            """
                SELECT COUNT(1) FROM overstyring o 
                INNER JOIN $tabell t on o.id = t.overstyring_ref 
                WHERE o.person_ref = (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer)
            """
        val antall =
            sessionOf(E2ETestApplikasjon.dbModule.dataSource, strict = true).use { session ->
                session.run(
                    queryOf(
                        query,
                        mapOf("fodselsnummer" to fødselsnummer()),
                    ).map { it.int(1) }.asSingle,
                )
            } ?: 0

        assertEquals(forventetAntall, antall)
    }
}
