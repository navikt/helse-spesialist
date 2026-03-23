package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgVeilederStansRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgVeilederStansRepository(session)

    @Test
    fun `kan lagre og finne alle veileder stanser for person`() {
        // Given
        val person = opprettPerson()
        val stans1 = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR),
            opprettet = Instant.now().minusSeconds(3600),
            originalMeldingId = UUID.randomUUID(),
        )
        val stans2 = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.AKTIVITETSKRAV),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )
        repository.lagre(stans1)
        repository.lagre(stans2)

        // When
        val alleStanser = repository.finnAlle(person.id)

        // Then
        assertEquals(2, alleStanser.size)
        // Sortert etter opprettet DESC, så nyeste først
        assertEquals(stans2.id, alleStanser[0].id)
        assertEquals(stans1.id, alleStanser[1].id)
    }

    @Test
    fun `finnAlle returnerer både aktive og opphevede stanser`() {
        // Given
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()

        val aktivStans = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )

        val opphevetStans = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.AKTIVITETSKRAV),
            opprettet = Instant.now().minusSeconds(3600),
            originalMeldingId = UUID.randomUUID(),
        )
        opphevetStans.opphevStans(
            opphevetAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Situasjonen er avklart",
        )

        repository.lagre(aktivStans)
        repository.lagre(opphevetStans)

        // When
        val alleStanser = repository.finnAlle(person.id)

        // Then
        assertEquals(2, alleStanser.size)
        val hentetAktiv = alleStanser.find { it.id == aktivStans.id }
        val hentetOpphevet = alleStanser.find { it.id == opphevetStans.id }

        assertNotNull(hentetAktiv)
        assertTrue(hentetAktiv.erStansett)

        assertNotNull(hentetOpphevet)
        assertFalse(hentetOpphevet.erStansett)
        assertNotNull(hentetOpphevet.stansOpphevet)
    }

    @Test
    fun `kan finne aktiv stans for person`() {
        // Given
        val person = opprettPerson()
        val veilederStans = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )
        repository.lagre(veilederStans)

        // When
        val aktivStans = repository.finnAktiv(person.id)

        // Then
        assertNotNull(aktivStans)
        assertEquals(veilederStans.id, aktivStans.id)
    }

    @Test
    fun `finner ikke aktiv stans hvis stansen er opphevet`() {
        // Given
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()
        val veilederStans = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )
        repository.lagre(veilederStans)

        veilederStans.opphevStans(
            opphevetAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Situasjonen er avklart",
        )
        repository.lagre(veilederStans)

        // When
        val aktivStans = repository.finnAktiv(person.id)

        // Then
        assertNull(aktivStans)
    }

    @Test
    fun `kan lagre og hente opphevet stans via finnAlle`() {
        // Given
        val person = opprettPerson()
        val saksbehandler = opprettSaksbehandler()
        val veilederStans = VeilederStans.ny(
            identitetsnummer = person.id,
            årsaker = setOf(StansÅrsak.MANGLENDE_MEDVIRKING),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )
        repository.lagre(veilederStans)

        // When
        veilederStans.opphevStans(
            opphevetAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Stansen oppheves",
        )
        repository.lagre(veilederStans)

        val alleStanser = repository.finnAlle(person.id)

        // Then
        assertEquals(1, alleStanser.size)
        val lagretStans = alleStanser.first()
        assertFalse(lagretStans.erStansett)
        assertNotNull(lagretStans.stansOpphevet)
        assertEquals(saksbehandler.ident, lagretStans.stansOpphevet!!.opphevetAvSaksbehandlerIdent)
        assertEquals("Stansen oppheves", lagretStans.stansOpphevet!!.begrunnelse)
    }

    @Test
    fun `finnAlle returnerer tom liste når person ikke har stanser`() {
        // Given
        val person = opprettPerson()

        // When
        val alleStanser = repository.finnAlle(person.id)

        // Then
        assertTrue(alleStanser.isEmpty())
    }

    @Test
    fun `finnAktiv returnerer null når stans ikke finnes`() {
        // Given
        val person = opprettPerson()

        // When
        val aktivStans = repository.finnAktiv(person.id)

        // Then
        assertNull(aktivStans)
    }
}


