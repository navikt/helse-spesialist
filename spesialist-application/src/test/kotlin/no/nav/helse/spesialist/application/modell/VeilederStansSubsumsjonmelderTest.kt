package no.nav.helse.spesialist.application.modell

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.stoppautomatiskbehandling.VeilederStansSubsumsjonmelder
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VeilederStans
import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak.AKTIVITETSKRAV
import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak.BESTRIDELSE_SYKMELDING
import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak.MANGLENDE_MEDVIRKING
import no.nav.helse.spesialist.domain.VeilederStans.StansÅrsak.MEDISINSK_VILKAR
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.UUID.randomUUID

class VeilederStansSubsumsjonmelderTest {
    private val meldingPubliserer = TestMeldingPubliserer()
    private val subsumsjonsmelder = Subsumsjonsmelder("versjonAvKode", meldingPubliserer)
    private val melder = VeilederStansSubsumsjonmelder { subsumsjonsmelder }

    private companion object {
        private val FNR = lagFødselsnummer()
        private val VEDTAKSPERIODEID = randomUUID()
        private const val ORGNR = "123456789"
    }

    @Test
    fun `sender subsumsjonsmelding med årsak medisinsk vilkår`() {
        melder.sendMelding(lagVeilederStans(MEDISINSK_VILKAR), FNR, ORGNR, VEDTAKSPERIODEID)

        val meldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, meldinger.size)

        val subsumsjon = meldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR), subsumsjon.input)
    }

    @Test
    fun `sender subsumsjonsmelding med årsak bestridelse sykmelding`() {
        melder.sendMelding(lagVeilederStans(BESTRIDELSE_SYKMELDING), FNR, ORGNR, VEDTAKSPERIODEID)

        val meldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, meldinger.size)

        val subsumsjon = meldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(mapOf("syfostopp" to true, "årsak" to BESTRIDELSE_SYKMELDING), subsumsjon.input)
    }

    @Test
    fun `sender subsumsjonsmelding med årsak aktivitetskrav`() {
        melder.sendMelding(lagVeilederStans(AKTIVITETSKRAV), FNR, ORGNR, VEDTAKSPERIODEID)

        val meldinger = meldingPubliserer.subsumsjonEvents().filter { it.paragraf == "8-8" }
        assertEquals(1, meldinger.size)

        val subsumsjon = meldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-8", subsumsjon.paragraf)
        assertEquals("2", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV), subsumsjon.input)
    }

    @Test
    fun `sender subsumsjonsmelding med årsak manglende medvirking`() {
        melder.sendMelding(lagVeilederStans(MANGLENDE_MEDVIRKING), FNR, ORGNR, VEDTAKSPERIODEID)

        val meldinger = meldingPubliserer.subsumsjonEvents().filter { it.paragraf == "8-8" }
        assertEquals(1, meldinger.size)

        val subsumsjon = meldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-8", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(mapOf("syfostopp" to true, "årsak" to MANGLENDE_MEDVIRKING), subsumsjon.input)
    }

    @Test
    fun `sender flere subsumsjonsmeldinger når det er flere årsaker`() {
        melder.sendMelding(lagVeilederStans(MEDISINSK_VILKAR, AKTIVITETSKRAV), FNR, ORGNR, VEDTAKSPERIODEID)

        val meldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(2, meldinger.size)

        val åtteFireSubsumsjon = meldinger.first { it.paragraf == "8-4" }
        assertEquals("8-4", åtteFireSubsumsjon.paragraf)
        assertEquals("1", åtteFireSubsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, åtteFireSubsumsjon.utfall)
        assertEquals(mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR), åtteFireSubsumsjon.input)

        val åtteÅtteSubsumsjon = meldinger.first { it.paragraf == "8-8" }
        assertEquals("8-8", åtteÅtteSubsumsjon.paragraf)
        assertEquals("2", åtteÅtteSubsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, åtteÅtteSubsumsjon.utfall)
        assertEquals(mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV), åtteÅtteSubsumsjon.input)
    }

    @Test
    fun `sender 8-4 oppfylt subsumsjon når veilederStans er null`() {
        melder.sendMelding(null, FNR, ORGNR, VEDTAKSPERIODEID)

        val meldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, meldinger.size)

        val subsumsjon = meldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertEquals(VILKAR_OPPFYLT.name, subsumsjon.utfall)
    }

    @Test
    fun `sender 8-4 oppfylt subsumsjon selv om automatisering er stanset på grunn av 8-8`() {
        melder.sendMelding(lagVeilederStans(AKTIVITETSKRAV), FNR, ORGNR, VEDTAKSPERIODEID)

        val åtteFireMeldinger = meldingPubliserer.subsumsjonEvents().filter { it.paragraf == "8-4" }
        assertEquals(1, åtteFireMeldinger.size)

        val subsumsjon = åtteFireMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertEquals(VILKAR_OPPFYLT.name, subsumsjon.utfall)
    }

    private fun lagVeilederStans(vararg årsaker: VeilederStans.StansÅrsak) =
        VeilederStans.ny(
            identitetsnummer = Identitetsnummer.fraString(FNR),
            årsaker = årsaker.toSet(),
            opprettet = Instant.now(),
            originalMeldingId = randomUUID(),
        )

    private class TestMeldingPubliserer : MeldingPubliserer {
        private val subsumsjonEvents = mutableListOf<SubsumsjonEvent>()

        fun subsumsjonEvents(): List<SubsumsjonEvent> = subsumsjonEvents

        override fun publiser(
            fødselsnummer: String,
            hendelse: UtgåendeHendelse,
            årsak: String,
        ) = error("Not implemented for test")

        override fun publiser(
            fødselsnummer: String,
            subsumsjonEvent: SubsumsjonEvent,
            versjonAvKode: String,
        ) {
            subsumsjonEvents.add(subsumsjonEvent)
        }

        override fun publiser(
            hendelseId: UUID,
            commandContextId: UUID,
            fødselsnummer: String,
            behov: List<Behov>,
            sti: List<Int>,
        ) = error("Not implemented for test")

        override fun publiser(
            fødselsnummer: String,
            event: KommandokjedeEndretEvent,
            hendelseNavn: String,
        ) = error("Not implemented for test")
    }
}
