package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.Test

class PgSaksbehandlerStansRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgSaksbehandlerStansRepository(session)
    private val person = opprettPerson()
    private val saksbehandler = opprettSaksbehandler()

    @Test
    fun `Kan lagre og finne ny saksbehandlerstans`() {
        // given
        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            identitetsnummer = person.id,
            begrunnelse = "begrunnelse"
        )

        repository.lagre(saksbehandlerStans)

        // when
        val lagretSaksbehandlerStans = repository.finn(person.id)

        // then
        assertNotNull(lagretSaksbehandlerStans)
        assertEquals(saksbehandlerStans.erStanset, lagretSaksbehandlerStans?.erStanset)
        assertEquals(saksbehandlerStans.versjon, lagretSaksbehandlerStans?.versjon)
        assertEquals(saksbehandlerStans.identitetsnummer, lagretSaksbehandlerStans?.identitetsnummer)
        assertEquals(saksbehandlerStans.events, lagretSaksbehandlerStans?.events)
    }

    @Test
    fun `Kan lagre og finne oppheving av saksbehandlerstans`() {
        // given
        val stans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            identitetsnummer = person.id,
            begrunnelse = "Oppretter stans"
        )
        repository.lagre(stans)

        // when
        stans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Opphever stans"
        )
        repository.lagre(stans)

        // then
        val hentetStans = repository.finn(person.id)
        assertNotNull(hentetStans)
        assertFalse(hentetStans!!.erStanset)
        assertEquals(2, hentetStans.versjon)
        assertEquals(2, hentetStans.events.size)
    }
}