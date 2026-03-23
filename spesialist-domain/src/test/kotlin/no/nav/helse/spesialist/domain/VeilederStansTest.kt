package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VeilederStansTest {

    @Test
    fun `kan opprette veileder stans`() {
        // Given
        val identitetsnummer = lagIdentitetsnummer()
        val årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR, StansÅrsak.AKTIVITETSKRAV)
        val opprettet = Instant.now()
        val originalMeldingId = UUID.randomUUID()

        // When
        val stans = VeilederStans.ny(
            identitetsnummer = identitetsnummer,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMeldingId = originalMeldingId,
        )

        // Then
        assertEquals(identitetsnummer, stans.identitetsnummer)
        assertEquals(årsaker, stans.årsaker)
        assertEquals(opprettet, stans.opprettet)
        assertEquals(originalMeldingId, stans.originalMeldingId)
        assertTrue(stans.erStansett)
        assertNull(stans.stansOpphevet)
    }

    @Test
    fun `kan oppheve veileder stans`() {
        // Given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandler = lagSaksbehandler()
        val begrunnelse = "Stans opphevet fordi situasjonen er avklart"

        val stans = VeilederStans.ny(
            identitetsnummer = identitetsnummer,
            årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )

        // When
        stans.opphevStans(
            opphevetAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = begrunnelse,
        )

        // Then
        assertNotNull(stans.stansOpphevet)
        assertFalse(stans.erStansett)
        assertEquals(saksbehandler.ident, stans.stansOpphevet!!.opphevetAvSaksbehandlerIdent)
        assertEquals(begrunnelse, stans.stansOpphevet!!.begrunnelse)
        with(stans.stansOpphevet!!.opphevetTidspunkt) {
            val now = Instant.now()
            assertTrue(
                isBefore(now) || this == now,
                "Forventet at opphevetTidspunkt var før eller lik nå ($now), men den var $this"
            )
            assertTrue(
                isAfter(now.minusSeconds(5)),
                "Forventet at opphevetTidspunkt var mindre enn fem sekunder før nå ($now), men den var $this"
            )
        }
    }

    @Test
    fun `kan opprette veileder stans fra lagring uten opphevet stans`() {
        // Given
        val id = VeilederStansId(UUID.randomUUID())
        val identitetsnummer = lagIdentitetsnummer()
        val årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR, StansÅrsak.AKTIVITETSKRAV)
        val opprettet = Instant.now().minusSeconds(3600)
        val originalMeldingId = UUID.randomUUID()

        // When
        val stans = VeilederStans.fraLagring(
            id = id,
            identitetsnummer = identitetsnummer,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMeldingId = originalMeldingId,
            stansOpphevet = null,
        )

        // Then
        assertEquals(id, stans.id)
        assertEquals(identitetsnummer, stans.identitetsnummer)
        assertEquals(årsaker, stans.årsaker)
        assertEquals(opprettet, stans.opprettet)
        assertEquals(originalMeldingId, stans.originalMeldingId)
        assertNull(stans.stansOpphevet)
    }

    @Test
    fun `kan opprette veileder stans fra lagring med opphevet stans`() {
        // Given
        val id = VeilederStansId(UUID.randomUUID())
        val identitetsnummer = lagIdentitetsnummer()
        val årsaker = setOf(StansÅrsak.MANGLENDE_MEDVIRKING)
        val opprettet = Instant.now().minusSeconds(3600)
        val originalMeldingId = UUID.randomUUID()
        val saksbehandler = lagSaksbehandler()
        val begrunnelse = "Stansen ble opphevet"
        val opphevetTidspunkt = Instant.now().minusSeconds(1800)

        // When
        val stans = VeilederStans.fraLagring(
            id = id,
            identitetsnummer = identitetsnummer,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMeldingId = originalMeldingId,
            stansOpphevet = VeilederStans.StansOpphevet(
                opphevetAvSaksbehandlerIdent = saksbehandler.ident,
                begrunnelse = begrunnelse,
                opphevetTidspunkt = opphevetTidspunkt,
            ),
        )

        // Then
        assertEquals(id, stans.id)
        assertFalse(stans.erStansett)
        assertNotNull(stans.stansOpphevet)
        assertEquals(saksbehandler.ident, stans.stansOpphevet!!.opphevetAvSaksbehandlerIdent)
        assertEquals(begrunnelse, stans.stansOpphevet!!.begrunnelse)
        assertEquals(opphevetTidspunkt, stans.stansOpphevet!!.opphevetTidspunkt)
    }

    @Test
    fun `ny veileder stans får unik id`() {
        // Given
        val identitetsnummer = lagIdentitetsnummer()

        // When
        val stans1 = VeilederStans.ny(
            identitetsnummer = identitetsnummer,
            årsaker = setOf(StansÅrsak.AKTIVITETSKRAV),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )
        val stans2 = VeilederStans.ny(
            identitetsnummer = identitetsnummer,
            årsaker = setOf(StansÅrsak.AKTIVITETSKRAV),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )

        // Then
        assertTrue(stans1.id != stans2.id, "Forventet at to nye VeilederStans fikk ulike id-er")
    }

    @Test
    fun `kan ikke oppheve stans som allerede er opphevet`() {
        // Given
        val saksbehandler = lagSaksbehandler()
        val stans = VeilederStans.ny(
            identitetsnummer = lagIdentitetsnummer(),
            årsaker = setOf(StansÅrsak.MEDISINSK_VILKAR),
            opprettet = Instant.now(),
            originalMeldingId = UUID.randomUUID(),
        )
        stans.opphevStans(
            opphevetAvSaksbehandlerIdent = saksbehandler.ident,
            begrunnelse = "Første oppheving",
        )

        // When/Then
        assertThrows<IllegalStateException> {
            stans.opphevStans(
                opphevetAvSaksbehandlerIdent = saksbehandler.ident,
                begrunnelse = "Andre oppheving",
            )
        }
    }
}