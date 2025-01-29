package no.nav.helse.modell.stoppautomatiskbehandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStanset
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.BESTRIDELSE_SYKMELDING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MANGLENDE_MEDVIRKING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID
import java.util.UUID.randomUUID

class StansAutomatiskBehandlingMediatorTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)

    private val meldingPubliserer = object : MeldingPubliserer {
        private val subsumsjonEvents: MutableList<SubsumsjonEvent> = mutableListOf()
        fun subsumsjonEvents(): List<SubsumsjonEvent> = subsumsjonEvents

        override fun publiser(fødselsnummer: String, hendelse: UtgåendeHendelse, årsak: String) =
            error("Not implemented for test")

        override fun publiser(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent, versjonAvKode: String) {
            subsumsjonEvents.add(subsumsjonEvent)
        }

        override fun publiser(
            hendelseId: UUID,
            commandContextId: UUID,
            fødselsnummer: String,
            behov: List<Behov>
        ) = error("Not implemented for test")

        override fun publiser(event: KommandokjedeEndretEvent, hendelseNavn: String) = error("Not implemented for test")
    }

    private val subsumsjonsmelder = Subsumsjonsmelder("versjonAvKode", meldingPubliserer)

    private companion object {
        private const val FNR = "12345678910"
        private val VEDTAKSPERIODEID = randomUUID()
        private const val ORGNR = "123456789"
    }

    private val mediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            periodehistorikkDao,
            oppgaveDao,
        ) { subsumsjonsmelder }

    @Test
    fun `Lagrer melding og periodehistorikk når stoppknapp-mleding håndteres`() {
        val melding =
            StansAutomatiskBehandlingMelding(
                id = randomUUID(),
                fødselsnummer = FNR,
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf(MEDISINSK_VILKAR),
                opprettet = now(),
                originalMelding = "{}",
                kilde = "ISYFO",
                json = "",
            )

        mediator.håndter(melding)

        verify(exactly = 1) { stansAutomatiskBehandlingDao.lagreFraISyfo(melding) }
        verify(exactly = 1) {
            periodehistorikkDao.lagreMedOppgaveId(
                historikkinnslag = any<AutomatiskBehandlingStanset>(),
                oppgaveId = any(),
            )
        }
    }

    @Test
    fun `Melding med status STOPP_AUTOMATIKK gjør at personen skal unntas fra automatisering`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MEDISINSK_VILKAR, MANGLENDE_MEDVIRKING),
                )
        assertTrue(mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR))
    }

    @Test
    fun `Melding med status NORMAL gjør at personen ikke lenger er unntatt fra automatisering`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MEDISINSK_VILKAR),
                    opphevStans(),
                )

        assertFalse(mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR))
    }

    @Test
    fun `Kan stanses på nytt etter stans er opphevet`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MEDISINSK_VILKAR),
                    opphevStans(),
                    stans(AKTIVITETSKRAV),
                )

        assertTrue(mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR))
    }

    @Test
    fun `sender subsumsjonsmelding med årsak medisinsk vilkår`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MEDISINSK_VILKAR),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR),
            subsumsjon.input,
        )
    }

    @Test
    fun `sender subsumsjonsmelding med årsak bestridelse sykmelding`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(BESTRIDELSE_SYKMELDING),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to BESTRIDELSE_SYKMELDING),
            subsumsjon.input,
        )
    }

    @Test
    fun `sender subsumsjonsmelding med årsak aktivitetskrav`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(AKTIVITETSKRAV),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents().filter { it.paragraf == "8-8" }
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-8", subsumsjon.paragraf)
        assertEquals("2", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV),
            subsumsjon.input,
        )
    }

    @Test
    fun `sender subsumsjonsmelding med årsak manglende medvirking`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MANGLENDE_MEDVIRKING),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents().filter { it.paragraf == "8-8" }
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-8", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertNull(subsumsjon.bokstav)
        assertEquals("folketrygdloven", subsumsjon.lovverk)
        assertEquals("2021-05-21", subsumsjon.lovverksversjon)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MANGLENDE_MEDVIRKING),
            subsumsjon.input,
        )
    }

    @Test
    fun `sender flere subsumsjonsmeldinger når det er flere stoppmeldinger med ulik årsak`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MEDISINSK_VILKAR),
                    stans(AKTIVITETSKRAV),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents()
        val åtteFireSubsumsjon = subsumsjonMeldinger.first()
        val åtteÅtteSubsumsjon = subsumsjonMeldinger.last()

        assertEquals(2, subsumsjonMeldinger.size)
        assertEquals("8-4", åtteFireSubsumsjon.paragraf)
        assertEquals("1", åtteFireSubsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, åtteFireSubsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR),
            åtteFireSubsumsjon.input,
        )

        assertEquals("8-8", åtteÅtteSubsumsjon.paragraf)
        assertEquals("2", åtteÅtteSubsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, åtteÅtteSubsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV),
            åtteÅtteSubsumsjon.input,
        )
    }

    @Test
    fun `sender flere subsumsjonsmeldinger når det er flere årsaker i samme stoppmelding`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(MEDISINSK_VILKAR, AKTIVITETSKRAV),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents()
        val åtteFireSubsumsjon = subsumsjonMeldinger.first()
        val åtteÅtteSubsumsjon = subsumsjonMeldinger.last()

        assertEquals(2, subsumsjonMeldinger.size)
        assertEquals("8-4", åtteFireSubsumsjon.paragraf)
        assertEquals("1", åtteFireSubsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, åtteFireSubsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR),
            åtteFireSubsumsjon.input,
        )

        assertEquals("8-8", åtteÅtteSubsumsjon.paragraf)
        assertEquals("2", åtteÅtteSubsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, åtteÅtteSubsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV),
            åtteÅtteSubsumsjon.input,
        )
    }

    @Test
    fun `sender bare subsumsjonsmelding for stoppmeldinger etter siste opphevelse av stans`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(AKTIVITETSKRAV),
                    opphevStans(),
                    stans(MEDISINSK_VILKAR),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertEquals(VILKAR_UAVKLART.name, subsumsjon.utfall)
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR),
            subsumsjon.input,
        )
    }

    @Test
    fun `sender 8-4 oppfylt subsumsjon når automatisering aldri er stanset`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns emptyList()

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents()
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertEquals(VILKAR_OPPFYLT.name, subsumsjon.utfall)
    }

    @Test
    fun `sender 8-4 oppfylt subsumsjon selv om automatisering er stanset på grunn av 8-8`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(AKTIVITETSKRAV),
                )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = meldingPubliserer.subsumsjonEvents().filter { it.paragraf == "8-4" }
        assertEquals(1, subsumsjonMeldinger.size)

        val subsumsjon = subsumsjonMeldinger.single()
        assertEquals(FNR, subsumsjon.fødselsnummer)
        assertEquals("8-4", subsumsjon.paragraf)
        assertEquals("1", subsumsjon.ledd)
        assertEquals(VILKAR_OPPFYLT.name, subsumsjon.utfall)
    }

    private fun stans(vararg årsaker: StoppknappÅrsak) = "STOPP_AUTOMATIKK" to årsaker.toSet()

    private fun opphevStans() = "NORMAL" to emptySet<StoppknappÅrsak>()

    private fun meldinger(vararg statusOgÅrsaker: Pair<String, Set<StoppknappÅrsak>>) =
        statusOgÅrsaker.map {
            StansAutomatiskBehandlingFraDatabase(
                FNR,
                it.first,
                it.second,
                now(),
                randomUUID().toString(),
            )
        }
}
