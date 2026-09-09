package no.nav.helse.spesialist.application

import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VedtaksperiodeDbDto
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.mar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VedtaksperiodeTest {
    @Test
    fun `sammenhengende - samme periode`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `sammenhengende - ligger tidligere enn og kant i kant og har samme skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(1 feb 2018, 28 feb 2018, 1 jan 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `sammenhengende - ligger tidligere enn og har opphold og har samme skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(1 mar 2018, 31 mar 2018, 1 jan 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `sammenhengende - opphold pa 18 dager og samme skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(18 feb 2018, 28 feb 2018, 1 jan 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `sammenhengende - opphold pa 18 dager selv med ulikt skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(18 feb 2018, 28 feb 2018, 1 feb 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `ikke sammenhengende - opphold pa 19 dager selv med ulikt skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(19 feb 2018, 28 feb 2018, 1 feb 2018)

        assertFalse(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `sammenhengende - opphold pa mer enn 18 dager og samme skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(19 feb 2018, 28 feb 2018, 1 jan 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `sammenhengende - samme skjæringstidspunkt og overlapper med én dag`() {
        val behandling1 = opprettApiVedtaksperiode(1 apr 2018, 30 apr 2018, 1 mar 2018)
        val behandling12 = opprettApiVedtaksperiode(1 mar 2018, 1 apr 2018, 1 mar 2018)

        assertTrue(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `ikke sammenhengende - ligger tidligere enn og ulikt skjæringstidspunkt`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val behandling12 = opprettApiVedtaksperiode(1 mar 2018, 31 mar 2018, 1 mar 2018)

        assertFalse(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    @Test
    fun `ikke sammenhengende - samme skjæringstidspunkt men ligger senere enn`() {
        val behandling1 = opprettApiVedtaksperiode(1 apr 2018, 30 apr 2018, 1 mar 2018)
        val behandling12 = opprettApiVedtaksperiode(1 mar 2018, 31 mar 2018, 1 mar 2018)

        assertFalse(behandling1.tidligereEnnOgSammenhengende(behandling12))
    }

    private fun opprettApiVedtaksperiode(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        varsler: List<VarselDbDto> = emptyList(),
    ): VedtaksperiodeDbDto = VedtaksperiodeDbDto(UUID.randomUUID(), fom, tom, skjæringstidspunkt, emptySet(), varsler.toSet())
}
