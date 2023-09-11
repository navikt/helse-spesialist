package no.nav.helse.mediator.oppgave

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.SØKNAD
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

    private val dao = mockk<OppgaveDao>(relaxed = true)
    private val testRapid = TestRapid()
    init {
        every { dao.finnFødselsnummer(any()) } returns FNR
    }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `bygg kafkamelding`() {
        val oppgave = nyOppgave()
        oppgave.register(Oppgavemelder(dao, testRapid))
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("oppgave_oppdatert", melding["@event_name"].asText())
        assertEquals(HENDELSE_ID, melding["hendelseId"].asUUID())
        assertEquals(HENDELSE_ID, melding["@forårsaket_av"]["id"].asUUID())
        assertEquals(OPPGAVE_ID, melding["oppgaveId"].asLong())
        assertEquals("AvventerSystem", melding["status"].asText())
        assertEquals("SØKNAD", melding["type"].asText())
        assertEquals(FNR, melding["fødselsnummer"].asText())
        assertEquals(false, melding["erBeslutterOppgave"].asBoolean())
        assertEquals(false, melding["erReturOppgave"].asBoolean())
    }

    private fun nyOppgave(medTotrinnsvurdering: Boolean = false) = Oppgave.oppgaveMedEgenskaper(
        id = OPPGAVE_ID,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        utbetalingId = UTBETALING_ID,
        hendelseId = HENDELSE_ID,
        egenskaper = listOf(SØKNAD),
        totrinnsvurdering = if (medTotrinnsvurdering) totrinnsvurdering() else null
    )

    private fun totrinnsvurdering() = Totrinnsvurdering(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        erRetur = false,
        saksbehandler = null,
        beslutter = null,
        utbetalingId = UUID.randomUUID(),
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now()
    )
}