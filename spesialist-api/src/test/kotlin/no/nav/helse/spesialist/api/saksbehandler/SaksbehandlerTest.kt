package no.nav.helse.spesialist.api.saksbehandler

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SaksbehandlerTest {
    private val oid = UUID.randomUUID()
    private val saksbehandler get() = Saksbehandler("epost@nav.no", oid, "navn", "ident")
    private val annulleringer = mutableListOf<String>()

    @Test
    fun `kan registrere observere`() {
        saksbehandler.apply {
            register(observer)
            annuller("aktørId", "fødselsnummer", "organisasjonsnummer", "fagsystemId", true, emptyList(), null)
        }
        assertEquals(1, annulleringer.size)
    }

    @Test
    fun `referential equals`() {
        val saksbehandler = saksbehandler
        assertEquals(saksbehandler, saksbehandler)
        assertEquals(saksbehandler.hashCode(), saksbehandler.hashCode())
    }

    @Test
    fun `structural equals`() {
        val saksbehandler1 = saksbehandler
        val saksbehandler2 = saksbehandler
        assertEquals(saksbehandler1, saksbehandler2)
        assertEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `forskjellig oid`() {
        val enSaksbehandler = saksbehandler
        val annenSaksbehandler = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "ident")
        assertNotEquals(annenSaksbehandler, enSaksbehandler)
        assertNotEquals(annenSaksbehandler.hashCode(), enSaksbehandler.hashCode())
    }

    @Test
    fun `forskjellig epost`() {
        val enSaksbehandler = saksbehandler
        val annenSaksbehandler = Saksbehandler("annen_epost@nav.no", oid, "navn", "ident")
        assertNotEquals(annenSaksbehandler, enSaksbehandler)
        assertNotEquals(annenSaksbehandler.hashCode(), enSaksbehandler.hashCode())
    }

    @Test
    fun `forskjellig navn`() {
        val enSaksbehandler = saksbehandler
        val annenSaksbehandler = Saksbehandler("epost@nav.no", oid, "annet navn", "ident")
        assertNotEquals(annenSaksbehandler, enSaksbehandler)
        assertNotEquals(annenSaksbehandler.hashCode(), enSaksbehandler.hashCode())
    }

    @Test
    fun `forskjellig ident`() {
        val enSaksbehandler = saksbehandler
        val annenSaksbehandler = Saksbehandler("epost@nav.no", oid, "navn", "annen_ident")
        assertNotEquals(annenSaksbehandler, enSaksbehandler)
        assertNotEquals(annenSaksbehandler.hashCode(), enSaksbehandler.hashCode())
    }

    private val observer = object: SaksbehandlerObserver {
        override fun annulleringEvent(
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            saksbehandler: Map<String, Any>,
            fagsystemId: String,
            begrunnelser: List<String>,
            gjelderSisteSkjæringstidspunkt: Boolean,
            kommentar: String?
        ) {
            annulleringer.add(fagsystemId)
        }
    }
}