package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class PgSaksbehandlerStansRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgSaksbehandlerStansRepository(session)

    @Test
    fun `Kan lagre og finne ny saksbehandlerstans`() {
        // given
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()

        val saksbehandlerStans =
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = saksbehandler.ident,
                identitetsnummer = person.id,
                begrunnelse = "begrunnelse",
            )

        repository.lagre(saksbehandlerStans)

        // when
        val lagretSaksbehandlerStans = repository.finn(person.id)

        // then
        assertNotNull(lagretSaksbehandlerStans)
        assertEquals(saksbehandlerStans.erStanset, lagretSaksbehandlerStans.erStanset)
        assertEquals(saksbehandlerStans.versjon, lagretSaksbehandlerStans.versjon)
        assertEquals(saksbehandlerStans.identitetsnummer, lagretSaksbehandlerStans.identitetsnummer)
        assertEquals(saksbehandlerStans.events.size, lagretSaksbehandlerStans.events.size)
    }

    @Test
    fun `Kan lagre og finne oppheving av saksbehandlerstans`() {
        // given
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()

        val stans =
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = saksbehandler.ident,
                identitetsnummer = person.id,
                begrunnelse = "Oppretter stans",
            )
        repository.lagre(stans)

        // when
        stans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Opphever stans",
        )
        repository.lagre(stans)

        // then
        val hentetStans = repository.finn(person.id)
        assertNotNull(hentetStans)
        assertFalse(hentetStans.erStanset)
        assertEquals(stans.versjon, hentetStans.versjon)
        assertEquals(stans.events.size, hentetStans.events.size)
    }
}
