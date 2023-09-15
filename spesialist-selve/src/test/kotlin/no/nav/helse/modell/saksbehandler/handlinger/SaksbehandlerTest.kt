package no.nav.helse.modell.saksbehandler.handlinger

import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.saksbehandler.handlinger.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.spesialist.api.modell.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.spesialist.api.modell.OverstyrtTidslinjeEvent
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
        saksbehandler.håndter(
            OverstyrtTidslinje(
                aktørId = "123",
                fødselsnummer = "1234",
                organisasjonsnummer = "12345",
                dager = emptyList(),
                begrunnelse = "begrunnelse"
            )
        )
        assertEquals(true, observert)
    }

    @Test
    fun `lager subsumsjoner ved håndtering av OverstyrtTidslinje`() {
        val subsumsjoner = mutableListOf<SubsumsjonEvent>()
        val observer = object : SaksbehandlerObserver {
            override fun nySubsumsjon(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent) {
                subsumsjoner.add(subsumsjonEvent)
            }
        }

        val saksbehandler = Saksbehandler("epost@nav.no", UUID.randomUUID(), "navn", "Z999999")
        saksbehandler.register(observer)
        saksbehandler.håndter(
            OverstyrtTidslinje(
                aktørId = "123",
                fødselsnummer = "1234",
                organisasjonsnummer = "12345",
                dager = overstyrteDager(),
                begrunnelse = "begrunnelse"
            )
        )

        assertEquals(2, subsumsjoner.size)
        assertEquals(
            SubsumsjonEvent(
                id = subsumsjoner[0].id,
                fødselsnummer = "1234",
                paragraf = "22-13",
                ledd = "7",
                bokstav = null,
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
                utfall = VILKAR_BEREGNET.name,
                output = mapOf(
                    "dager" to listOf(
                        mapOf(
                            "dato" to 1.januar,
                            "type" to "Sykedag",
                            "fraType" to "Sykedag",
                            "grad" to 100,
                            "fraGrad" to 100,
                        ),
                        mapOf(
                            "dato" to 2.januar,
                            "type" to "Sykedag",
                            "fraType" to "Sykedag",
                            "grad" to 100,
                            "fraGrad" to 100,
                        ),
                    )
                ),
                input = mapOf("begrunnelseFraSaksbehandler" to "begrunnelse"),
                sporing = mapOf("organisasjonsnummer" to listOf("12345"), "vedtaksperiode" to emptyList()),
                tidsstempel = subsumsjoner[0].tidsstempel,
                kilde = "spesialist",
            ),
            subsumsjoner[0]
        )
        assertEquals(
            SubsumsjonEvent(
                id = subsumsjoner[1].id,
                fødselsnummer = "1234",
                paragraf = "ANNEN PARAGRAF",
                ledd = "ANNET LEDD",
                bokstav = "EN BOKSTAV",
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
                utfall = VILKAR_BEREGNET.name,
                output = mapOf(
                    "dager" to listOf(
                        mapOf(
                            "dato" to 3.januar,
                            "type" to "Sykedag",
                            "fraType" to "Sykedag",
                            "grad" to 100,
                            "fraGrad" to 100,
                        ),
                    )
                ),
                input = mapOf("begrunnelseFraSaksbehandler" to "begrunnelse"),
                sporing = mapOf("organisasjonsnummer" to listOf("12345"), "vedtaksperiode" to emptyList()),
                tidsstempel = subsumsjoner[1].tidsstempel,
                kilde = "spesialist",
            ),
            subsumsjoner[1]
        )
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

    private fun overstyrteDager(): List<OverstyrtTidslinjedag> = listOf(
        OverstyrtTidslinjedag(
            dato = 1.januar,
            type = "Sykedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = Lovhjemmel(
                paragraf = "22-13",
                ledd = "7",
            )
        ),
        OverstyrtTidslinjedag(
            dato = 2.januar,
            type = "Sykedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = Lovhjemmel(
                paragraf = "22-13",
                ledd = "7",
            )
        ),
        OverstyrtTidslinjedag(
            dato = 3.januar,
            type = "Sykedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = Lovhjemmel(
                paragraf = "ANNEN PARAGRAF",
                ledd = "ANNET LEDD",
                bokstav = "EN BOKSTAV",
            )
        ),
        OverstyrtTidslinjedag(
            dato = 4.januar,
            type = "Feriedag",
            fraType = "Sykedag",
            grad = 100,
            fraGrad = 100,
            lovhjemmel = null,
        )
    )
}