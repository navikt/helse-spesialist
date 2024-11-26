package no.nav.helse.modell.stoppautomatiskbehandling

import TilgangskontrollForTestHarIkkeTilgang
import com.fasterxml.jackson.module.kotlin.convertValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PgDialogDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStanset
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.OpphevStans
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.BESTRIDELSE_SYKMELDING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MANGLENDE_MEDVIRKING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import no.nav.helse.objectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID

class StansAutomatiskBehandlingMediatorTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val notatDao = mockk<NotatDao>(relaxed = true)
    private val dialogDao = mockk<PgDialogDao>(relaxed = true)
    private val testRapid = TestRapid()
    private val subsumsjonsmelder = Subsumsjonsmelder("versjonAvKode", testRapid)

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
            notatDao,
            dialogDao,
        ) { subsumsjonsmelder }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

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
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        val oid = randomUUID()
        mediator.håndter(
            handling = OpphevStans(FNR, "begrunnelse"),
            saksbehandler =
                Saksbehandler(
                    epostadresse = "epost",
                    oid = oid,
                    navn = "navn",
                    ident = "ident",
                    tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
                ),
        )

        verify(exactly = 1) { stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = FNR) }
        verify(exactly = 1) {
            notatDao.lagreForOppgaveId(
                oppgaveId = any(),
                tekst = "begrunnelse",
                saksbehandlerOid = oid,
                notatType = NotatType.OpphevStans,
                dialogRef = any(),
            )
        }
    }

    @Test
    fun `Melding med status STOPP_AUTOMATIKK gjør at personen skal unntas fra automatisering`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(MEDISINSK_VILKAR, MANGLENDE_MEDVIRKING),
            )
        val dataTilSpeil = mediator.unntattFraAutomatiskGodkjenning(FNR)

        assertTrue(mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR))
        assertTrue(dataTilSpeil.erUnntatt)
        assertEquals(listOf(MEDISINSK_VILKAR.name, MANGLENDE_MEDVIRKING.name), dataTilSpeil.arsaker)
    }

    @Test
    fun `Melding med status NORMAL gjør at personen ikke lenger er unntatt fra automatisering`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(MEDISINSK_VILKAR),
                opphevStans(),
            )
        val dataTilSpeil = mediator.unntattFraAutomatiskGodkjenning(FNR)

        assertFalse(mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR))
        assertFalse(dataTilSpeil.erUnntatt)
        assertEquals(emptyList<String>(), dataTilSpeil.arsaker)
    }

    @Test
    fun `Kan stanses på nytt etter stans er opphevet`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(MEDISINSK_VILKAR),
                opphevStans(),
                stans(AKTIVITETSKRAV),
            )
        val dataTilSpeil = mediator.unntattFraAutomatiskGodkjenning(FNR)

        assertTrue(mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR))
        assertTrue(dataTilSpeil.erUnntatt)
        assertEquals(listOf(AKTIVITETSKRAV.name), dataTilSpeil.arsaker)
    }

    @Test
    fun `sender subsumsjonsmelding med årsak medisinsk vilkår`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(MEDISINSK_VILKAR),
            )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon").filter { it.path("subsumsjon")["paragraf"].asText() == "8-4" }
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals(FNR, subsumsjon["fodselsnummer"].asText())
        assertEquals("versjonAvKode", subsumsjon["versjonAvKode"].asText())
        assertEquals("1.0.0", subsumsjon["versjon"].asText())
        assertEquals("8-4", subsumsjon["paragraf"].asText())
        assertEquals("1", subsumsjon["ledd"].asText())
        assertNull(subsumsjon["bokstav"])
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asText())
        assertEquals("2021-05-21", subsumsjon["lovverksversjon"].asText())
        assertEquals(VILKAR_UAVKLART.name, subsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR.name),
            objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]),
        )
    }

    @Test
    fun `sender subsumsjonsmelding med årsak bestridelse sykmelding`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(BESTRIDELSE_SYKMELDING),
            )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon").filter { it.path("subsumsjon")["paragraf"].asText() == "8-4" }
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals(FNR, subsumsjon["fodselsnummer"].asText())
        assertEquals("versjonAvKode", subsumsjon["versjonAvKode"].asText())
        assertEquals("1.0.0", subsumsjon["versjon"].asText())
        assertEquals("8-4", subsumsjon["paragraf"].asText())
        assertEquals("1", subsumsjon["ledd"].asText())
        assertNull(subsumsjon["bokstav"])
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asText())
        assertEquals("2021-05-21", subsumsjon["lovverksversjon"].asText())
        assertEquals(VILKAR_UAVKLART.name, subsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to BESTRIDELSE_SYKMELDING.name),
            objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]),
        )
    }

    @Test
    fun `sender subsumsjonsmelding med årsak aktivitetskrav`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(AKTIVITETSKRAV),
            )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon").filter { it.path("subsumsjon")["paragraf"].asText() == "8-8" }
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals(FNR, subsumsjon["fodselsnummer"].asText())
        assertEquals("versjonAvKode", subsumsjon["versjonAvKode"].asText())
        assertEquals("1.0.0", subsumsjon["versjon"].asText())
        assertEquals("8-8", subsumsjon["paragraf"].asText())
        assertEquals("2", subsumsjon["ledd"].asText())
        assertNull(subsumsjon["bokstav"])
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asText())
        assertEquals("2021-05-21", subsumsjon["lovverksversjon"].asText())
        assertEquals(VILKAR_UAVKLART.name, subsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV.name),
            objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]),
        )
    }

    @Test
    fun `sender subsumsjonsmelding med årsak manglende medvirking`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(MANGLENDE_MEDVIRKING),
            )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon").filter { it.path("subsumsjon")["paragraf"].asText() == "8-8" }
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals(FNR, subsumsjon["fodselsnummer"].asText())
        assertEquals("versjonAvKode", subsumsjon["versjonAvKode"].asText())
        assertEquals("1.0.0", subsumsjon["versjon"].asText())
        assertEquals("8-8", subsumsjon["paragraf"].asText())
        assertEquals("1", subsumsjon["ledd"].asText())
        assertNull(subsumsjon["bokstav"])
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asText())
        assertEquals("2021-05-21", subsumsjon["lovverksversjon"].asText())
        assertEquals(VILKAR_UAVKLART.name, subsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MANGLENDE_MEDVIRKING.name),
            objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]),
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

        val subsumsjonMeldinger = testRapid.inspektør.hendelser("subsumsjon")
        val åtteFireSubsumsjon = subsumsjonMeldinger.first().path("subsumsjon")
        val åtteÅtteSubsumsjon = subsumsjonMeldinger.last().path("subsumsjon")

        assertEquals(2, subsumsjonMeldinger.size)
        assertEquals("8-4", åtteFireSubsumsjon["paragraf"].asText())
        assertEquals("1", åtteFireSubsumsjon["ledd"].asText())
        assertEquals(VILKAR_UAVKLART.name, åtteFireSubsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR.name),
            objectMapper.convertValue<Map<String, Any>>(åtteFireSubsumsjon["input"]),
        )

        assertEquals("8-8", åtteÅtteSubsumsjon["paragraf"].asText())
        assertEquals("2", åtteÅtteSubsumsjon["ledd"].asText())
        assertEquals(VILKAR_UAVKLART.name, åtteÅtteSubsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV.name),
            objectMapper.convertValue<Map<String, Any>>(åtteÅtteSubsumsjon["input"]),
        )
    }

    @Test
    fun `sender flere subsumsjonsmeldinger når det er flere årsaker i samme stoppmelding`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(MEDISINSK_VILKAR, AKTIVITETSKRAV),
            )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger = testRapid.inspektør.hendelser("subsumsjon")
        val åtteFireSubsumsjon = subsumsjonMeldinger.first().path("subsumsjon")
        val åtteÅtteSubsumsjon = subsumsjonMeldinger.last().path("subsumsjon")

        assertEquals(2, subsumsjonMeldinger.size)
        assertEquals("8-4", åtteFireSubsumsjon["paragraf"].asText())
        assertEquals("1", åtteFireSubsumsjon["ledd"].asText())
        assertEquals(VILKAR_UAVKLART.name, åtteFireSubsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR.name),
            objectMapper.convertValue<Map<String, Any>>(åtteFireSubsumsjon["input"]),
        )

        assertEquals("8-8", åtteÅtteSubsumsjon["paragraf"].asText())
        assertEquals("2", åtteÅtteSubsumsjon["ledd"].asText())
        assertEquals(VILKAR_UAVKLART.name, åtteÅtteSubsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to AKTIVITETSKRAV.name),
            objectMapper.convertValue<Map<String, Any>>(åtteÅtteSubsumsjon["input"]),
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

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon").filter { it.path("subsumsjon")["paragraf"].asText() == "8-4" }
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals("8-4", subsumsjon["paragraf"].asText())
        assertEquals("1", subsumsjon["ledd"].asText())
        assertEquals(VILKAR_UAVKLART.name, subsumsjon["utfall"].asText())
        assertEquals(
            mapOf("syfostopp" to true, "årsak" to MEDISINSK_VILKAR.name),
            objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]),
        )
    }

    @Test
    fun `sender 8-4 oppfylt subsumsjon når automatisering aldri er stanset`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns emptyList()

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon")
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals(FNR, subsumsjon["fodselsnummer"].asText())
        assertEquals("8-4", subsumsjon["paragraf"].asText())
        assertEquals("1", subsumsjon["ledd"].asText())
        assertEquals(VILKAR_OPPFYLT.name, subsumsjon["utfall"].asText())
    }

    @Test
    fun `sender 8-4 oppfylt subsumsjon selv om automatisering er stanset på grunn av 8-8`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
            meldinger(
                stans(AKTIVITETSKRAV),
            )

        mediator.sjekkOmAutomatiseringErStanset(FNR, VEDTAKSPERIODEID, ORGNR)

        val subsumsjonMeldinger =
            testRapid.inspektør.hendelser("subsumsjon").filter { it.path("subsumsjon")["paragraf"].asText() == "8-4" }
        val subsumsjon = subsumsjonMeldinger.first().path("subsumsjon")

        assertEquals(1, subsumsjonMeldinger.size)
        assertEquals(FNR, subsumsjon["fodselsnummer"].asText())
        assertEquals("8-4", subsumsjon["paragraf"].asText())
        assertEquals("1", subsumsjon["ledd"].asText())
        assertEquals(VILKAR_OPPFYLT.name, subsumsjon["utfall"].asText())
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
