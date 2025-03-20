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
        val generasjon1 = opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val generasjon2 = opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `sammenhengende - ligger tidligere enn og kant i kant og har samme skjæringstidspunkt`() {
        val generasjon1 = opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val generasjon2 = opprettApiGenerasjon(1 feb 2018, 28 feb 2018, 1 jan 2018)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `sammenhengende - ligger tidligere enn og har opphold og har samme skjæringstidspunkt`() {
        val generasjon1 = opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val generasjon2 = opprettApiGenerasjon(1 mar 2018, 31 mar 2018, 1 jan 2018)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `sammenhengende - samme skjæringstidspunkt og overlapper med én dag`() {
        val generasjon1 = opprettApiGenerasjon(1 apr 2018, 30 apr 2018, 1 mar 2018)
        val generasjon2 = opprettApiGenerasjon(1 mar 2018, 1 apr 2018, 1 mar 2018)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `ikke sammenhengende - ligger tidligere enn og ulikt skjæringstidspunkt`() {
        val generasjon1 = opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018)
        val generasjon2 = opprettApiGenerasjon(1 mar 2018, 31 mar 2018, 1 mar 2018)

        assertFalse(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `ikke sammenhengende - samme skjæringstidspunkt men ligger senere enn`() {
        val generasjon1 = opprettApiGenerasjon(1 apr 2018, 30 apr 2018, 1 mar 2018)
        val generasjon2 = opprettApiGenerasjon(1 mar 2018, 31 mar 2018, 1 mar 2018)

        assertFalse(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `har aktive varsler`() {
        val generasjon1 =
            opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.AKTIV)))
        val generasjon2 =
            opprettApiGenerasjon(1 feb 2018, 28 feb 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.AKTIV)))

        assertTrue(setOf(generasjon1, generasjon2).harAktiveVarsler())
    }

    @Test
    fun `har ikke aktive varsler`() {
        val generasjon1 =
            opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.GODKJENT)))
        val generasjon2 =
            opprettApiGenerasjon(1 feb 2018, 28 feb 2018, 1 jan 2018, listOf(opprettVarsel(Varselstatus.AVVIST)))

        assertFalse(setOf(generasjon1, generasjon2).harAktiveVarsler())
    }

    @Test
    fun `har ikke aktive varsler hvis generasjon ikke har varsler`() {
        val generasjon1 = opprettApiGenerasjon(1 jan 2018, 31 jan 2018, 1 jan 2018)

        assertFalse(setOf(generasjon1).harAktiveVarsler())
    }

    private fun opprettApiGenerasjon(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, varsler: List<VarselDbDto> = emptyList()): VedtaksperiodeDbDto {
        return VedtaksperiodeDbDto(UUID.randomUUID(), fom, tom, skjæringstidspunkt, varsler.toSet())
    }

    private fun opprettVarsel(status: Varselstatus) = VarselDbDto(
        varselId = UUID.randomUUID(),
        generasjonId = UUID.randomUUID(),
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
