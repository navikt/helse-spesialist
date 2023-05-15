package no.nav.helse.spesialist.api.vedtak

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.april
import no.nav.helse.spesialist.api.februar
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.mars
import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import no.nav.helse.spesialist.api.vedtak.ApiGenerasjon.Companion.harAktiveVarsler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ApiGenerasjonTest {

    @Test
    fun `sammenhengende - samme periode`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar)
        val generasjon2 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `sammenhengende - ligger tidligere enn og kant i kant og har samme skjæringstidspunkt`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar)
        val generasjon2 = opprettApiGenerasjon(1.februar, 28.februar, 1.januar)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `sammenhengende - ligger tidligere enn og har opphold og har samme skjæringstidspunkt`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar)
        val generasjon2 = opprettApiGenerasjon(1.mars, 31.mars, 1.januar)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `sammenhengende - samme skjæringstidspunkt og overlapper med én dag`() {
        val generasjon1 = opprettApiGenerasjon(1.april, 30.april, 1.mars)
        val generasjon2 = opprettApiGenerasjon(1.mars, 1.april, 1.mars)

        assertTrue(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `ikke sammenhengende - ligger tidligere enn og ulikt skjæringstidspunkt`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar)
        val generasjon2 = opprettApiGenerasjon(1.mars, 31.mars, 1.mars)

        assertFalse(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `ikke sammenhengende - samme skjæringstidspunkt men ligger senere enn`() {
        val generasjon1 = opprettApiGenerasjon(1.april, 30.april, 1.mars)
        val generasjon2 = opprettApiGenerasjon(1.mars, 31.mars, 1.mars)

        assertFalse(generasjon1.tidligereEnnOgSammenhengende(generasjon2))
    }

    @Test
    fun `har aktive varsler`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar, listOf(opprettVarsel(Varselstatus.AKTIV)))
        val generasjon2 = opprettApiGenerasjon(1.februar, 28.februar, 1.januar, listOf(opprettVarsel(Varselstatus.AKTIV)))

        assertTrue(setOf(generasjon1, generasjon2).harAktiveVarsler())
    }

    @Test
    fun `har ikke aktive varsler`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar, listOf(opprettVarsel(Varselstatus.GODKJENT)))
        val generasjon2 = opprettApiGenerasjon(1.februar, 28.februar, 1.januar, listOf(opprettVarsel(Varselstatus.AVVIST)))

        assertFalse(setOf(generasjon1, generasjon2).harAktiveVarsler())
    }

    @Test
    fun `har ikke aktive varsler hvis generasjon ikke har varsler`() {
        val generasjon1 = opprettApiGenerasjon(1.januar, 31.januar, 1.januar)

        assertFalse(setOf(generasjon1).harAktiveVarsler())
    }

    private fun opprettApiGenerasjon(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, varsler: List<Varsel> = emptyList()): ApiGenerasjon {
        return ApiGenerasjon(UUID.randomUUID(), fom, tom, skjæringstidspunkt, varsler.toSet())
    }

    private fun opprettVarsel(status: Varselstatus): Varsel {
        return Varsel(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", status, "EN_TITTEL", null, null, null)
    }
}