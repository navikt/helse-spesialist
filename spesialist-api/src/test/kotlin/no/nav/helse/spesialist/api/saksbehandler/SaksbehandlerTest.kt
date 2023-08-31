package no.nav.helse.spesialist.api.saksbehandler

import java.util.UUID
import no.nav.helse.spesialist.api.januar
import no.nav.helse.spesialist.api.modell.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.spesialist.api.modell.OverstyrtTidslinjeEvent
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.modell.SaksbehandlerObserver
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class SaksbehandlerTest {

    @Test
    fun `håndtering av OverstyrtTidslinje medfører utgående event`() {
        var observert = false
        val observer = object : SaksbehandlerObserver {
            override fun tidslinjeOverstyrt(fødselsnummer: String, event: OverstyrtTidslinjeEvent) {
                observert = true
            }
        }

        val saksbehandler = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "Z999999")
        saksbehandler.register(observer)
        saksbehandler.håndter(OverstyrtTidslinje("123", "1234", "12345", emptyList(), "begrunnelse"))
        assertEquals(true, observert)
    }

    @Test
    fun `håndtering av OverstyrtInntektOgRefusjon medfører utgående event`() {
        var observert = false
        val observer = object : SaksbehandlerObserver {
            override fun inntektOgRefusjonOverstyrt(fødselsnummer: String, event: OverstyrtInntektOgRefusjonEvent) {
                observert = true
            }
        }

        val saksbehandler = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "Z999999")
        saksbehandler.register(observer)
        saksbehandler.håndter(OverstyrtInntektOgRefusjon("123", "1234", 1.januar, emptyList()))
        assertEquals(true, observert)
    }

    @Test
    fun `referential equals`() {
        val saksbehandler = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "Z999999")
        assertEquals(saksbehandler, saksbehandler)
        assertEquals(saksbehandler.hashCode(), saksbehandler.hashCode())
    }

    @Test
    fun `structural equals`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = Saksbehandler("epost@nav.no", oid, "navn", "Z999999")
        val saksbehandler2 = Saksbehandler("epost@nav.no", oid, "navn", "Z999999")
        assertEquals(saksbehandler1, saksbehandler2)
        assertEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - epost`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = Saksbehandler("epost1@nav.no", oid, "navn", "Z999999")
        val saksbehandler2 = Saksbehandler("epost2@nav.no", oid, "navn", "Z999999")
        assertNotEquals(saksbehandler1, saksbehandler2)
        assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - oid`() {
        val saksbehandler1 = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "Z999999")
        val saksbehandler2 = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "Z999999")
        assertNotEquals(saksbehandler1, saksbehandler2)
        assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - navn`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = Saksbehandler("epost@nav.no", oid, "navn 1", "Z999999")
        val saksbehandler2 = Saksbehandler("epost@nav.no", oid, "navn 2", "Z999999")
        assertNotEquals(saksbehandler1, saksbehandler2)
        assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - ident`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = Saksbehandler("epost@nav.no", oid, "navn", "X999999")
        val saksbehandler2 = Saksbehandler("epost@nav.no", oid, "navn", "Y999999")
        assertNotEquals(saksbehandler1, saksbehandler2)
        assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }
}