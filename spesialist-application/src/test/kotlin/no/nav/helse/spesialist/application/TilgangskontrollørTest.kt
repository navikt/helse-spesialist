package no.nav.helse.spesialist.application

import no.nav.helse.Gruppekontroll
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.Gruppe.KODE7
import no.nav.helse.spesialist.api.bootstrap.Gruppe.SKJERMEDE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.EnumSet
import java.util.UUID

class TilgangskontrollørTest {
    private val forespørsler = mutableMapOf<UUID, List<String>>()

    private val gruppekontroll =
        object : Gruppekontroll {
            override suspend fun erIGrupper(
                oid: UUID,
                gruppeIder: List<UUID>,
            ): Boolean {
                forespørsler[oid] = gruppeIder.map { it.toString() }
                return true
            }
        }

    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(gruppekontroll, tilgangsgrupper)

    @Test
    fun `Har tilgang hvis liste er tom`() {
        assertTrue(tilgangskontrollørForReservasjon.harTilgangTil(UUID.randomUUID(), emptyList()))
    }

    @Test
    fun `Mapper EGEN_ANSATT til gruppeId for egenansatt`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollørForReservasjon.harTilgangTil(saksbehandlerOid, listOf(EGEN_ANSATT))
        assertEquals(listOf(idForGruppe(SKJERMEDE)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper FORTROLIG_ADRESSE til gruppeId for kode 7`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollørForReservasjon.harTilgangTil(saksbehandlerOid, listOf(FORTROLIG_ADRESSE))
        assertEquals(listOf(idForGruppe(KODE7)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper BESLUTTER til gruppeId for beslutter`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollørForReservasjon.harTilgangTil(saksbehandlerOid, listOf(BESLUTTER))
        assertEquals(listOf(idForGruppe(Gruppe.BESLUTTER)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper STIKKPRØVE til gruppeId for stikkprøve`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollørForReservasjon.harTilgangTil(saksbehandlerOid, listOf(STIKKPRØVE))
        assertEquals(listOf(idForGruppe(Gruppe.STIKKPRØVE)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper alle tilgangsstyrte egenskaper`() {
        val saksbehandlerOid = UUID.randomUUID()
        Egenskap.alleTilgangsstyrteEgenskaper.forEach {
            assertDoesNotThrow {
                tilgangskontrollørForReservasjon.harTilgangTil(saksbehandlerOid, listOf(it))
            }
        }
    }

    @Test
    fun `har tilgang dersom saksbehandler har tilstrekkelige tilganger`() {
        val saksbehandlergrupper = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) }
        val tilgangskontrollørForApi = TilgangskontrollørForApi(saksbehandlergrupper, tilgangsgrupper)
        Egenskap.alleTilgangsstyrteEgenskaper
            .filterNot { it == STRENGT_FORTROLIG_ADRESSE } // Ingen har tilgang til disse i Speil foreløpig
            .forEach {
                assertEquals(true, tilgangskontrollørForApi.harTilgangTil(UUID.randomUUID(), listOf(it)))
            }
    }

    @Test
    fun `har ikke tilgang dersom saksbehandler mangler minst én tilgang`() {
        val saksbehandlergrupper = EnumSet.allOf(Gruppe::class.java).map { UUID.fromString(idForGruppe(it)) }.dropLast(1)
        val tilgangskontrollørForApi = TilgangskontrollørForApi(saksbehandlergrupper, tilgangsgrupper)
        assertEquals(false, tilgangskontrollørForApi.harTilgangTil(UUID.randomUUID(), Egenskap.alleTilgangsstyrteEgenskaper))
    }
}

private fun idForGruppe(gruppe: Gruppe): String = tilgangsgrupper.gruppeId(gruppe).toString()
