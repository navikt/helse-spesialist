package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaksbehandlerStansTest {

    @Test
    fun `kan opprette saksbehandlerstans`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val begrunnelse = "begrunnelse"


        // when
        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = begrunnelse,
        )

        // then
        assertTrue(saksbehandlerStans.erStanset)
        assertEquals(1, saksbehandlerStans.events.size)
        assertEquals(SaksbehandlerStansOpprettetEvent::class, saksbehandlerStans.events.last()::class)

        val stansOpprettetEvent = saksbehandlerStans.events.last() as SaksbehandlerStansOpprettetEvent
        assertEquals(1, stansOpprettetEvent.metadata.sekvensnummer)
        assertEquals(begrunnelse, stansOpprettetEvent.metadata.begrunnelse)
        assertEquals(saksbehandlerIdent, stansOpprettetEvent.metadata.utførtAvSaksbehandlerIdent)
        with(stansOpprettetEvent.metadata.tidspunkt) {
            val now = Instant.now()
            assertTrue(
                isBefore(now),
                "Forventet at den lagrede verdien av tidspunkt var før nå ($now), men den var $this"
            )
            assertTrue(
                isAfter(now.minusSeconds(5)),
                "Forventet at den lagrede verdien av tidspunkt var mindre enn fem sekunder før nå ($now), men den var $this"
            )
        }
    }

    @Test
    fun `kan opprette og oppheve stans`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val begrunnelse = "begrunnelse to"


        // when
        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse en",
        )

        saksbehandlerStans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            begrunnelse = begrunnelse,
        )

        // then
        assertFalse(saksbehandlerStans.erStanset)
        assertEquals(identitetsnummer, saksbehandlerStans.identitetsnummer)
        assertEquals(2, saksbehandlerStans.events.size)
        assertEquals(SaksbehandlerStansOpphevetEvent::class, saksbehandlerStans.events.last()::class)

        val stansOpphevetEvent = saksbehandlerStans.events.last() as SaksbehandlerStansOpphevetEvent
        assertEquals(2, stansOpphevetEvent.metadata.sekvensnummer)
        assertEquals(2, saksbehandlerStans.versjon)
        assertEquals(begrunnelse, stansOpphevetEvent.metadata.begrunnelse)
        assertEquals(saksbehandlerIdent, stansOpphevetEvent.metadata.utførtAvSaksbehandlerIdent)
        with(stansOpphevetEvent.metadata.tidspunkt) {
            val now = Instant.now()
            assertTrue(
                isBefore(now),
                "Forventet at den lagrede verdien av tidspunkt var før nå ($now), men den var $this"
            )
            assertTrue(
                isAfter(now.minusSeconds(5)),
                "Forventet at den lagrede verdien av tidspunkt var mindre enn fem sekunder før nå ($now), men den var $this"
            )
        }
    }

    @Test
    fun `kan opprette, oppheve og opprette stans`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident
        val begrunnelse = "begrunnelse tre"


        // when
        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse en",
        )

        saksbehandlerStans.opphevStans(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            begrunnelse = "begrunnelse to",
        )

        saksbehandlerStans.opprettStans(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            begrunnelse = begrunnelse,
        )

        // then
        assertTrue(saksbehandlerStans.erStanset)
        assertEquals(identitetsnummer, saksbehandlerStans.identitetsnummer)
        assertEquals(3, saksbehandlerStans.events.size)
        assertEquals(SaksbehandlerStansOpprettetEvent::class, saksbehandlerStans.events.last()::class)

        val stansOpprettetEvent = saksbehandlerStans.events.last() as SaksbehandlerStansOpprettetEvent
        assertEquals(3, stansOpprettetEvent.metadata.sekvensnummer)
        assertEquals(3, saksbehandlerStans.versjon)
        assertEquals(begrunnelse, stansOpprettetEvent.metadata.begrunnelse)
        assertEquals(saksbehandlerIdent, stansOpprettetEvent.metadata.utførtAvSaksbehandlerIdent)
    }

    @Test
    fun `Ikke lov å opprette stans på eksisterende stans`() {
        // given
        val identitetsnummer = lagIdentitetsnummer()
        val saksbehandlerIdent = lagSaksbehandler().ident

        val saksbehandlerStans = SaksbehandlerStans.ny(
            utførtAvSaksbehandlerIdent = saksbehandlerIdent,
            identitetsnummer = identitetsnummer,
            begrunnelse = "begrunnelse",
        )

        // then when
        assertThrows<RuntimeException> { saksbehandlerStans.opprettStans(utførtAvSaksbehandlerIdent = saksbehandlerIdent, begrunnelse = "begrunnelse") }
    }

    @Test
    fun `Ikke lov å oppheve stans på allerede opphevet stans`() {
        // given
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

        // then when
        assertThrows<RuntimeException> { saksbehandlerStans.opphevStans(utførtAvSaksbehandlerIdent = saksbehandlerIdent, begrunnelse = "begrunnelse") }
    }
}