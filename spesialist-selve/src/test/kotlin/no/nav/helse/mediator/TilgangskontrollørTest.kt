package no.nav.helse.mediator

import java.util.UUID
import no.nav.helse.Gruppe
import no.nav.helse.Gruppe.KODE7
import no.nav.helse.Gruppe.SKJERMEDE
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.idForGruppe
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TilgangskontrollørTest {

    private val tilgangsgrupper = Tilgangsgrupper(testEnv)

    private val forespørsler = mutableMapOf<UUID, List<String>>()

    private val gruppekontroll = object : Gruppekontroll {
        override suspend fun erIGrupper(oid: UUID, gruppeIder: List<UUID>): Boolean {
            forespørsler[oid] = gruppeIder.map { it.toString() }
            return true
        }
    }

    private val tilgangskontrollør = Tilgangskontrollør(gruppekontroll, tilgangsgrupper)

    @Test
    fun `Mapper EGEN_ANSATT til gruppeId for egenansatt`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(EGEN_ANSATT))
        assertEquals(listOf(idForGruppe(SKJERMEDE)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper RISK_QA til gruppeId for risk QA`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(RISK_QA))
        assertEquals(listOf(idForGruppe(Gruppe.RISK_QA)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper FORTROLIG_ADRESSE til gruppeId for kode 7`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(FORTROLIG_ADRESSE))
        assertEquals(listOf(idForGruppe(KODE7)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper BESLUTTER til gruppeId for beslutter`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(BESLUTTER))
        assertEquals(listOf(idForGruppe(Gruppe.BESLUTTER)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper STIKKPRØVE til gruppeId for stikkprøve`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(STIKKPRØVE))
        assertEquals(listOf(idForGruppe(Gruppe.STIKKPRØVE)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper SPESIALSAK til gruppeId for spesialsak`() {
        val saksbehandlerOid = UUID.randomUUID()
        tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(SPESIALSAK))
        assertEquals(listOf(idForGruppe(Gruppe.SPESIALSAK)), forespørsler[saksbehandlerOid])
    }

    @Test
    fun `Mapper alle tilgangsstyrte egenskaper`() {
        val saksbehandlerOid = UUID.randomUUID()
        Egenskap.alleTilgangsstyrteEgenskaper.forEach {
            assertDoesNotThrow {
                tilgangskontrollør.harTilgangTil(saksbehandlerOid, listOf(it))
            }
        }
    }
}