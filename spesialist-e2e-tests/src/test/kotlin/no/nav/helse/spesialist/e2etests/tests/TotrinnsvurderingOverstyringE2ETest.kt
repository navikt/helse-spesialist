package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.api.rest.ApiLovhjemmel
import no.nav.helse.spesialist.api.rest.ApiOverstyrArbeidsforholdRequest
import no.nav.helse.spesialist.api.rest.ApiOverstyrInntektOgRefusjonRequest
import no.nav.helse.spesialist.api.rest.ApiOverstyrTidslinjeRequest
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TotrinnsvurderingOverstyringE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av inntekt og refusjon`() {
        val vedtaksperiode =
            førsteVedtaksperiode().apply {
                fom = 1 jan 2021
                tom = 31 jan 2021
            }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerInntektOgRefusjon(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                skjæringstidspunkt = vedtaksperiode.fom,
                arbeidsgivere =
                    listOf(
                        ApiOverstyrInntektOgRefusjonRequest.Arbeidsgiver(
                            organisasjonsnummer = organisasjonsnummer(),
                            månedligInntekt = 30000.0,
                            fraMånedligInntekt = 25000.0,
                            refusjonsopplysninger = emptyList(),
                            fraRefusjonsopplysninger = emptyList(),
                            begrunnelse = "begrunnelse",
                            forklaring = "forklaring",
                            lovhjemmel = null,
                            fom = null,
                            tom = null,
                        ),
                    ),
            )
        }
        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertTotrinnsvurderingHarAktivOverstyring()
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av arbeidsforhold`() {
        val orgnrGhost = lagOrganisasjonsnummer()
        val vedtaksperiode =
            førsteVedtaksperiode().apply {
                fom = 1 jan 2021
                tom = 31 jan 2021
            }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn(orgnummereMedRelevanteArbeidsforhold = listOf(orgnrGhost))

        medPersonISpeil {
            saksbehandlerOverstyrerArbeidsforhold(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                skjæringstidspunkt = vedtaksperiode.fom,
                overstyrteArbeidsforhold =
                    listOf(
                        ApiOverstyrArbeidsforholdRequest.Arbeidsforhold(
                            organisasjonsnummer = orgnrGhost,
                            deaktivert = true,
                            begrunnelse = "begrunnelse",
                            forklaring = "forklaring",
                            lovhjemmel = ApiLovhjemmel("8-15", null, null, "folketrygdloven", "1998-12-18"),
                        ),
                    ),
            )
        }
        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertTotrinnsvurderingHarAktivOverstyring()
    }

    @Test
    fun `sak blir trukket til totrinnsvurdering ved overstyring av tidslinje`() {
        val vedtaksperiode =
            førsteVedtaksperiode().apply {
                fom = 1 jan 2021
                tom = 31 jan 2021
            }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerTidslinje(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                dager =
                    listOf(
                        ApiOverstyrTidslinjeRequest.Dag(
                            dato = vedtaksperiode.fom,
                            type = "Sykedag",
                            fraType = "Feriedag",
                            grad = 100,
                            fraGrad = null,
                            lovhjemmel = null,
                        ),
                    ),
            )
        }
        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        assertTotrinnsvurderingHarAktivOverstyring()
    }

    @Test
    fun `totrinnsvurdering får vedtaksperiodeForkastet satt til true hvis vedtaksperioden overstyringen er knyttet til blir forkastet`() {
        val vedtaksperiode =
            førsteVedtaksperiode().apply {
                fom = 1 jan 2021
                tom = 31 jan 2021
            }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerOverstyrerTidslinje(
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
                dager =
                    listOf(
                        ApiOverstyrTidslinjeRequest.Dag(
                            dato = vedtaksperiode.fom,
                            type = "Sykedag",
                            fraType = "Feriedag",
                            grad = 100,
                            fraGrad = null,
                            lovhjemmel = null,
                        ),
                    ),
            )
        }
        spleisReberegnerAutomatisk(vedtaksperiode)
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        spleisKasterUtSaken(vedtaksperiode)

        assertTotrinnsvurderingErForkastet()
    }

    private fun assertTotrinnsvurderingHarAktivOverstyring() {
        val antall =
            dbQuery.singleOrNull(
                """
                select count(*) as antall
                from overstyring o
                    inner join totrinnsvurdering tv on tv.id = o.totrinnsvurdering_ref
                    inner join person p on p.id = tv.person_ref
                where p.fødselsnummer = :fodselsnummer
                    and tv.tilstand = 'AVVENTER_SAKSBEHANDLER'
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer(),
            ) { it.int("antall") }
        assertTrue((antall ?: 0) > 0) {
            "Forventet at det finnes minst én aktiv overstyring knyttet til totrinnsvurderingen for fødselsnummer=${fødselsnummer()}"
        }
    }

    private fun assertTotrinnsvurderingErForkastet() {
        val vedtaksperiodeForkastet =
            dbQuery.singleOrNull(
                """
                select tv.vedtaksperiode_forkastet
                from totrinnsvurdering tv
                    inner join person p on p.id = tv.person_ref
                where p.fødselsnummer = :fodselsnummer
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer(),
            ) { it.boolean("vedtaksperiode_forkastet") }
                ?: error("Finner ikke totrinnsvurdering for fødselsnummer=${fødselsnummer()}")
        assertTrue(vedtaksperiodeForkastet) {
            "Forventer at totrinnsvurdering er markert som forkastet"
        }
    }
}
