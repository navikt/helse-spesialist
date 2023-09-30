package no.nav.helse.modell.saksbehandler.handlinger

import java.util.UUID
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.saksbehandler.OverstyrtTidslinjeEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.SubsumsjonEvent
import org.junit.jupiter.api.Assertions
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

        val saksbehandler = saksbehandler()
        saksbehandler.register(observer)
        saksbehandler.håndter(
            OverstyrtTidslinje(
                vedtaksperiodeId = UUID.randomUUID(),
                aktørId = "123",
                fødselsnummer = "1234",
                organisasjonsnummer = "12345",
                dager = emptyList(),
                begrunnelse = "begrunnelse",
            )
        )
        Assertions.assertEquals(true, observert)
    }

    @Test
    fun `lager subsumsjoner ved håndtering av OverstyrtTidslinje`() {
        val subsumsjoner = mutableListOf<SubsumsjonEvent>()
        val observer = object : SaksbehandlerObserver {
            override fun nySubsumsjon(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent) {
                subsumsjoner.add(subsumsjonEvent)
            }
        }

        val saksbehandler = saksbehandler()
        val vedtaksperiodeId = UUID.randomUUID()
        val overstyringId = UUID.randomUUID()
        saksbehandler.register(observer)
        saksbehandler.håndter(
            OverstyrtTidslinje(
                id = overstyringId,
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = "123",
                fødselsnummer = "1234",
                organisasjonsnummer = "12345",
                dager = overstyrteDager(),
                begrunnelse = "begrunnelse",
            )
        )

        Assertions.assertEquals(2, subsumsjoner.size)
        Assertions.assertEquals(
            SubsumsjonEvent(
                id = subsumsjoner[0].id,
                fødselsnummer = "1234",
                paragraf = "22-13",
                ledd = "7",
                bokstav = null,
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
                utfall = Subsumsjon.Utfall.VILKAR_BEREGNET.name,
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
                sporing = mapOf(
                    "organisasjonsnummer" to listOf("12345"),
                    "vedtaksperiode" to listOf(vedtaksperiodeId.toString()),
                    "saksbehandler" to listOf("epost@nav.no"),
                    "overstyrtidslinje" to listOf(overstyringId.toString()),
                ),
                tidsstempel = subsumsjoner[0].tidsstempel,
                kilde = "spesialist",
            ),
            subsumsjoner[0]
        )
        Assertions.assertEquals(
            SubsumsjonEvent(
                id = subsumsjoner[1].id,
                fødselsnummer = "1234",
                paragraf = "ANNEN PARAGRAF",
                ledd = "ANNET LEDD",
                bokstav = "EN BOKSTAV",
                lovverk = "ANNEN LOV",
                lovverksversjon = "1970-01-01",
                utfall = Subsumsjon.Utfall.VILKAR_BEREGNET.name,
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
                sporing = mapOf(
                    "organisasjonsnummer" to listOf("12345"),
                    "vedtaksperiode" to listOf(vedtaksperiodeId.toString()),
                    "saksbehandler" to listOf("epost@nav.no"),
                    "overstyrtidslinje" to listOf(overstyringId.toString()),
                ),
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

        val saksbehandler = saksbehandler()
        saksbehandler.register(observer)
        saksbehandler.håndter(OverstyrtInntektOgRefusjon("123", "1234", 1.januar, emptyList()))
        Assertions.assertEquals(true, observert)
    }

    @Test
    fun `tildeling med påVent=true`() {
        val oid = UUID.randomUUID()
        val epost = "augunn.saksbehandler@nav.no"
        val navn = "Augunn Saksbehandler"
        val ident = "S123456"
        val saksbehandler = saksbehandler(epost, oid, navn, ident)
        val tildeling = saksbehandler.tildeling(true)
        Assertions.assertEquals(oid, tildeling.oid)
        Assertions.assertEquals(navn, tildeling.navn)
        Assertions.assertEquals(epost, tildeling.epost)
        Assertions.assertEquals(true, tildeling.påVent)
    }

    @Test
    fun `tildeling med påVent=false`() {
        val oid = UUID.randomUUID()
        val epost = "augunn.saksbehandler@nav.no"
        val navn = "Augunn Saksbehandler"
        val ident = "S123456"
        val saksbehandler = saksbehandler(epost, oid, navn, ident)
        val tildeling = saksbehandler.tildeling(false)
        Assertions.assertEquals(oid, tildeling.oid)
        Assertions.assertEquals(navn, tildeling.navn)
        Assertions.assertEquals(epost, tildeling.epost)
        Assertions.assertEquals(false, tildeling.påVent)
    }

    @Test
    fun `referential equals`() {
        val saksbehandler = saksbehandler()
        Assertions.assertEquals(saksbehandler, saksbehandler)
        Assertions.assertEquals(saksbehandler.hashCode(), saksbehandler.hashCode())
    }

    @Test
    fun `structural equals`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = saksbehandler(oid = oid)
        val saksbehandler2 = saksbehandler(oid = oid)
        Assertions.assertEquals(saksbehandler1, saksbehandler2)
        Assertions.assertEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - epost`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = saksbehandler(epost = "epost1@nav.no", oid = oid)
        val saksbehandler2 = saksbehandler(epost = "epost2@nav.no", oid = oid)
        Assertions.assertNotEquals(saksbehandler1, saksbehandler2)
        Assertions.assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - oid`() {
        val saksbehandler1 = saksbehandler()
        val saksbehandler2 = saksbehandler()
        Assertions.assertNotEquals(saksbehandler1, saksbehandler2)
        Assertions.assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - navn`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = saksbehandler(navn = "navn 1", oid = oid)
        val saksbehandler2 = saksbehandler(navn = "navn 2", oid = oid)
        Assertions.assertNotEquals(saksbehandler1, saksbehandler2)
        Assertions.assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
    }

    @Test
    fun `not equals - ident`() {
        val oid = UUID.randomUUID()
        val saksbehandler1 = saksbehandler(ident = "X999999", oid = oid)
        val saksbehandler2 = saksbehandler(ident = "Y999999", oid = oid)
        Assertions.assertNotEquals(saksbehandler1, saksbehandler2)
        Assertions.assertNotEquals(saksbehandler1.hashCode(), saksbehandler2.hashCode())
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
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
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
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-06-21",
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
                lovverk = "ANNEN LOV",
                lovverksversjon = "1970-01-01",
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

    private fun saksbehandler(
        epost: String = "epost@nav.no",
        oid: UUID = UUID.randomUUID(),
        navn: String = "navn",
        ident: String = "Z999999",
    ) = Saksbehandler(
        epostadresse = epost,
        oid = oid,
        navn = navn,
        ident = ident,
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
    )
}