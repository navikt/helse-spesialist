package no.nav.helse.mediator.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.MeldingPubliserer
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong

class OppgavemelderTest {

    private companion object {
        private val FNR = lagFødselsnummer()
        private val OPPGAVE_ID = nextLong()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val BEHANDLING_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
    }

    private val testRapid = TestRapid()
    private val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(testRapid)
    private val saksbehandler = saksbehandler("saksbehandler@nav.no")
    private val beslutter = saksbehandler("beslutter@nav.no")

    @Test
    fun `bygg kafkamelding`() {
        val oppgave = nyOppgave()
        oppgave.register(Oppgavemelder(FNR, meldingPubliserer))
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("oppgave_oppdatert", melding["@event_name"].asText())
        assertEquals(HENDELSE_ID, melding["hendelseId"].asUUID())
        assertEquals(HENDELSE_ID, melding["@forårsaket_av"]["id"].asUUID())
        assertEquals(OPPGAVE_ID, melding["oppgaveId"].asLong())
        assertEquals(BEHANDLING_ID, melding["behandlingId"].asUUID())
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
        oppgave.register(Oppgavemelder(FNR, meldingPubliserer))
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()

        assertEquals("oppgave_oppdatert", melding["@event_name"].asText())
        assertEquals(HENDELSE_ID, melding["hendelseId"].asUUID())
        assertEquals(HENDELSE_ID, melding["@forårsaket_av"]["id"].asUUID())
        assertEquals(OPPGAVE_ID, melding["oppgaveId"].asLong())
        assertEquals(BEHANDLING_ID, melding["behandlingId"].asUUID())
        assertEquals("AvventerSystem", melding["tilstand"].asText())
        assertEquals(FNR, melding["fødselsnummer"].asText())
        assertEquals(saksbehandler.oid(), melding["saksbehandler"].asUUID())
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
        behandlingId = BEHANDLING_ID,
        utbetalingId = UTBETALING_ID,
        hendelseId = HENDELSE_ID,
        egenskaper = setOf(SØKNAD),
        totrinnsvurdering = totrinnsvurdering,
        kanAvvises = true,
    )

    private fun totrinnsvurdering(beslutter: Saksbehandler? = null) = Totrinnsvurdering.fraLagring(
        id = TotrinnsvurderingId(nextLong()),
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        erRetur = false,
        saksbehandler = null,
        beslutter = beslutter,
        utbetalingId = UUID.randomUUID(),
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        overstyringer = emptyList(),
        ferdigstilt = false,
    )
}
