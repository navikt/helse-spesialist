package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaksbehandlerStansTest {

    @Test
    fun `kan opprette saksbehandlerstans`() {
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val begrunnelse = "begrunnelse"

        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = begrunnelse,
        )

        assertTrue(saksbehandlerStans.erStanset)
        assertNull(saksbehandlerStans.stansOpphevet)
        assertEquals(identitetsnummer, saksbehandlerStans.identitetsnummer)
        assertEquals(saksbehandlerIdent, saksbehandlerStans.utførtAv)
        assertEquals(begrunnelse, saksbehandlerStans.begrunnelse)
        assertNotNull(saksbehandlerStans.id)
        with(saksbehandlerStans.opprettet) {
            val now = Instant.now()
            assertTrue(isBefore(now), "Forventet at opprettet var før nå ($now), men den var $this")
            assertTrue(isAfter(now.minusSeconds(5)), "Forventet at opprettet var mindre enn fem sekunder før nå")
        }
    }

    @Test
    fun `kan opprette og oppheve stans`() {
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident

        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse en",
        )

        val opphevBegrunnelse = "begrunnelse to"
        saksbehandlerStans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            begrunnelse = opphevBegrunnelse,
        )

        assertFalse(saksbehandlerStans.erStanset)
        assertEquals(identitetsnummer, saksbehandlerStans.identitetsnummer)
        assertNotNull(saksbehandlerStans.stansOpphevet).also { opphevet ->
            assertEquals(saksbehandlerIdent, opphevet.utførtAv)
            assertEquals(opphevBegrunnelse, opphevet.begrunnelse)
            with(opphevet.tidspunkt) {
                val now = Instant.now()
                assertTrue(isBefore(now), "Forventet at opphevet tidspunkt var før nå")
                assertTrue(isAfter(now.minusSeconds(5)), "Forventet at opphevet tidspunkt var nylig")
            }
        }
    }

    @Test
    fun `re-stans oppretter ny instans med ny id`() {
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident

        val førstStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse en",
        )
        førstStans.opphevStans(utførtAvSaksbehandlerIdent = saksbehandlerIdent, begrunnelse = "begrunnelse to")

        val andreStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse tre",
        )

        assertTrue(andreStans.erStanset)
        assertNull(andreStans.stansOpphevet)
        assertEquals(identitetsnummer, andreStans.identitetsnummer)
        assertTrue(førstStans.id != andreStans.id, "Re-stans skal ha ny unik id")
    }

    @Test
    fun `Ikke lov å oppheve stans på allerede opphevet stans`() {
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident

        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse",
        )
        saksbehandlerStans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            begrunnelse = "begrunnelse",
        )

        assertThrows<RuntimeException> {
            saksbehandlerStans.opphevStans(utførtAvSaksbehandlerIdent = saksbehandlerIdent, begrunnelse = "begrunnelse")
        }
    }
}