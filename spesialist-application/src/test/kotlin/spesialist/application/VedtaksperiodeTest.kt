package no.nav.helse.spesialist.application

import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VarselDbDto.Varselstatus
import no.nav.helse.db.api.VedtaksperiodeDbDto
import no.nav.helse.db.api.VedtaksperiodeDbDto.Companion.harAktiveVarsler
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.mar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
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

    @Test
    fun `har aktive varsler`() {
        val behandling1 =
            opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.AKTIV)))
        val behandling12 =
            opprettApiVedtaksperiode(1 feb 2018, 28 feb 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.AKTIV)))

        assertTrue(setOf(behandling1, behandling12).harAktiveVarsler())
    }

    @Test
    fun `har ikke aktive varsler`() {
        val behandling1 =
            opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.GODKJENT)))
        val behandling12 =
            opprettApiVedtaksperiode(1 feb 2018, 28 feb 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.AVVIST)))

        assertFalse(setOf(behandling1, behandling12).harAktiveVarsler())
    }

    @Test
    fun `har ikke aktive varsler hvis vedtaksperioden ikke har varsler`() {
        val behandling1 = opprettApiVedtaksperiode(1 jan 2018, 31 jan 2018, 1 jan 2018)

        assertFalse(setOf(behandling1).harAktiveVarsler())
    }

    private fun opprettApiVedtaksperiode(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, varsler: List<VarselDbDto> = emptyList()): VedtaksperiodeDbDto {
        return VedtaksperiodeDbDto(UUID.randomUUID(), fom, tom, skjæringstidspunkt, emptySet(),  varsler.toSet())
    }

    private fun opprettVarsel(status: Varselstatus) = VarselDbDto(
        varselId = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        opprettet = LocalDateTime.now(),
        kode = "SB_EX_1",
        status = status,
        varseldefinisjon = VarselDbDto.VarseldefinisjonDbDto(
            definisjonId = UUID.randomUUID(),
            tittel = "EN_TITTEL",
            forklaring = null,
            handling = null,
        ),
        varselvurdering = null,
    )
}
