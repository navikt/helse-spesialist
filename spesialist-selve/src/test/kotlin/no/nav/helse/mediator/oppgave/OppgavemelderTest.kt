package no.nav.helse.mediator.oppgave

import TilgangskontrollForTestHarIkkeTilgang
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgavemelderTest {

    private companion object {
        private const val FNR = "12345678910"
        private const val OPPGAVE_ID = 1L
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
    }

    private val meldingDao = mockk<MeldingDao>(relaxed = true)
    private val testRapid = TestRapid()
    private val saksbehandler = saksbehandler("saksbehandler@nav.no")
    private val beslutter = saksbehandler("beslutter@nav.no")
    init {
        every { meldingDao.finnFødselsnummer(any()) } returns FNR
    }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `bygg kafkamelding`() {
        val oppgave = nyOppgave()
        oppgave.register(Oppgavemelder(meldingDao, testRapid))
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("oppgave_oppdatert", melding["@event_name"].asText())
        assertEquals(HENDELSE_ID, melding["hendelseId"].asUUID())
        assertEquals(HENDELSE_ID, melding["@forårsaket_av"]["id"].asUUID())
        assertEquals(OPPGAVE_ID, melding["oppgaveId"].asLong())
        assertEquals("AvventerSystem", melding["tilstand"].asText())
        assertEquals(FNR, melding["fødselsnummer"].asText())
        assertEquals(null, melding["beslutter"])
        assertEquals(null, melding["saksbehandler"])
        assertEquals(listOf("SØKNAD"), melding["egenskaper"].map { it.asText() })
    }

    @Test
    fun `bygg kafkamelding med saksbehandler og beslutter`() {
        val oppgave = nyOppgave(totrinnsvurdering = totrinnsvurdering(beslutter))
        oppgave.forsøkTildelingVedReservasjon(saksbehandler = saksbehandler)
        oppgave.register(Oppgavemelder(meldingDao, testRapid))
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("oppgave_oppdatert", melding["@event_name"].asText())
        assertEquals(HENDELSE_ID, melding["hendelseId"].asUUID())
        assertEquals(HENDELSE_ID, melding["@forårsaket_av"]["id"].asUUID())
        assertEquals(OPPGAVE_ID, melding["oppgaveId"].asLong())
        assertEquals("AvventerSystem", melding["tilstand"].asText())
        assertEquals(FNR, melding["fødselsnummer"].asText())
        assertEquals(beslutter.epostadresse(), melding["beslutter"]["epostadresse"].asText())
        assertEquals(beslutter.oid(), melding["beslutter"]["oid"].asUUID())
        assertEquals(saksbehandler.epostadresse(), melding["saksbehandler"]["epostadresse"].asText())
        assertEquals(saksbehandler.oid(), melding["saksbehandler"]["oid"].asUUID())
        assertEquals(listOf("SØKNAD"), melding["egenskaper"].map { it.asText() })
    }

    private fun saksbehandler(epostadresse: String) = Saksbehandler(
        epostadresse = epostadresse,
        oid = UUID.randomUUID(),
        navn = "En Saksbehandler",
        ident = "S123456",
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang
    )

    private fun nyOppgave(totrinnsvurdering: Totrinnsvurdering? = null) = Oppgave.nyOppgave(
        id = OPPGAVE_ID,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        utbetalingId = UTBETALING_ID,
        hendelseId = HENDELSE_ID,
        egenskaper = listOf(SØKNAD),
        totrinnsvurdering = totrinnsvurdering,
        kanAvvises = true,
    )

    private fun totrinnsvurdering(beslutter: Saksbehandler? = null) = Totrinnsvurdering(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        erRetur = false,
        saksbehandler = null,
        beslutter = beslutter,
        utbetalingId = UUID.randomUUID(),
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now()
    )
}