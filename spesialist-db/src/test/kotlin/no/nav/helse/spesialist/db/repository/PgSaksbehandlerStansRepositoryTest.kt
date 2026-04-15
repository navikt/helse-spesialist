package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgSaksbehandlerStansRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgSaksbehandlerStansRepository(session)

    @Test
    fun `Kan lagre og finne aktiv saksbehandlerstans`() {
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()

        val stans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            identitetsnummer = person.id,
            begrunnelse = "begrunnelse",
        )
        repository.lagre(stans)

        val lagretStans = repository.finnAktiv(person.id)

        assertNotNull(lagretStans)
        assertTrue(lagretStans.erStanset)
        assertNull(lagretStans.stansOpphevet)
        assertEquals(stans.id, lagretStans.id)
        assertEquals(stans.identitetsnummer, lagretStans.identitetsnummer)
        assertEquals(stans.utførtAv, lagretStans.utførtAv)
        assertEquals(stans.begrunnelse, lagretStans.begrunnelse)
    }

    @Test
    fun `Kan lagre og finne oppheving av saksbehandlerstans`() {
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()

        val stans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            identitetsnummer = person.id,
            begrunnelse = "Oppretter stans",
        )
        repository.lagre(stans)

        stans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Opphever stans",
        )
        repository.lagre(stans)

        assertNull(repository.finnAktiv(person.id))

        val alle = repository.finnAlle(person.id)
        assertEquals(1, alle.size)
        assertFalse(alle.first().erStanset)
        assertNotNull(alle.first().stansOpphevet).also { opphevet ->
            assertEquals(saksbehandler.ident, opphevet.utførtAv)
            assertEquals("Opphever stans", opphevet.begrunnelse)
        }
    }

    @Test
    fun `Re-stans oppretter ny rad og bevarer historikk`() {
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()

        val førstStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            identitetsnummer = person.id,
            begrunnelse = "Første stans",
        )
        repository.lagre(førstStans)

        førstStans.opphevStans(utførtAvSaksbehandlerIdent = saksbehandler.ident, begrunnelse = "Opphever")
        repository.lagre(førstStans)

        val andreStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            identitetsnummer = person.id,
            begrunnelse = "Andre stans",
        )
        repository.lagre(andreStans)

        val aktiv = repository.finnAktiv(person.id)
        assertNotNull(aktiv)
        assertTrue(aktiv.erStanset)
        assertEquals(andreStans.id, aktiv.id)
        assertEquals("Andre stans", aktiv.begrunnelse)

        val alle = repository.finnAlle(person.id)
        assertEquals(2, alle.size)
    }
}
