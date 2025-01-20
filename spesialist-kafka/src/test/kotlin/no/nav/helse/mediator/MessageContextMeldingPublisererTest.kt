package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.HentDokument
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.OppgaveOpprettet
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class MessageContextMeldingPublisererTest {
    private val testRapid: TestRapid = TestRapid()
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val contextId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val fødselsnummer = lagFødselsnummer()
    private val årsak = "JUnit"

    @Test
    fun `publiserer behov med forventet format`() {
        val fom = LocalDate.now().minusDays(1)
        val tom = LocalDate.now()
        val behov = Behov.Infotrygdutbetalinger(fom, tom)

        meldingPubliserer.publiser(
            hendelseId = hendelseId,
            commandContextId = contextId,
            fødselsnummer = fødselsnummer,
            behov = listOf(behov)
        )

        assertTrue(!testRapid.inspektør.field(0, "@behov").isMissingOrNull())
        assertEquals("behov", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(fødselsnummer, testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals(contextId.toString(), testRapid.inspektør.field(0, "contextId").asText())
        assertEquals(hendelseId.toString(), testRapid.inspektør.field(0, "hendelseId").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
    }

    @Test
    fun `publiserer hendelse med forventet format`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val periodetype = "FORLENGELSE"
        val utgåendeHendelse = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periodetype = periodetype
        )

        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = utgåendeHendelse,
            årsak = årsak,
        )

        assertEquals("vedtaksperiode_godkjent", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(fødselsnummer, testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals(vedtaksperiodeId.toString(), testRapid.inspektør.field(0, "vedtaksperiodeId").asText())
        assertEquals(behandlingId.toString(), testRapid.inspektør.field(0, "behandlingId").asText())
        assertEquals(periodetype, testRapid.inspektør.field(0, "periodetype").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
    }

    @Test
    fun `publiserer KommandokjedeEndretEvent med forventet format`() {
        val event = KommandokjedeEndretEvent.Avbrutt(
            commandContextId = contextId,
            hendelseId = hendelseId,
        )

        meldingPubliserer.publiser(
            event = event,
            hendelseNavn = årsak,
        )

        assertEquals("kommandokjede_avbrutt", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(contextId.toString(), testRapid.inspektør.field(0, "commandContextId").asText())
        assertEquals(hendelseId.toString(), testRapid.inspektør.field(0, "meldingId").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["SØKNAD", "INNTEKTSMELDING"])
    fun `publiserer HentDokument med forventet format`(dokumentType: String) {
        val dokumentId = UUID.randomUUID()

        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = HentDokument(dokumentId = dokumentId, dokumentType = dokumentType),
            årsak = årsak,
        )

        assertEquals(1, testRapid.inspektør.size)
        testRapid.inspektør.message(0).let {
            assertEquals("hent-dokument", it["@event_name"].asText())
            assertEquals(dokumentId, UUID.fromString(it["dokumentId"].asText()))
            assertEquals(dokumentType, it["dokumentType"].asText())
        }
    }

    @Test
    fun `publiserer OppgaveOpprettet med forventet format`() {
        // Given:
        val oppgaveId = nextLong()
        val behandlingId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val oppgave = Oppgave.nyOppgave(
            id = oppgaveId,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = UUID.randomUUID(),
            hendelseId = hendelseId,
            kanAvvises = true,
            egenskaper = setOf(Egenskap.SØKNAD),
        )

        // When:
        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = OppgaveOpprettet(oppgave = oppgave),
            årsak = årsak,
        )

        // Then:
        assertEquals(1, testRapid.inspektør.size)
        testRapid.inspektør.message(index = 0).let {
            assertEquals("oppgave_opprettet", it["@event_name"].asText())

            it["@forårsaket_av"]?.let { forårsaketAvObject ->
                assertEquals(hendelseId, UUID.fromString(forårsaketAvObject["id"]?.asText()))
            } ?: fail("Mangler @forårsaket_av")

            assertEquals(hendelseId, UUID.fromString(it["hendelseId"]?.asText()))
            assertEquals(fødselsnummer, it["fødselsnummer"].asText())
            assertEquals(oppgaveId, it["oppgaveId"]?.longValue())
            assertEquals(Oppgavestatus.AvventerSaksbehandler, enumValueOf<Oppgavestatus>(it["tilstand"].asText()))
            assertEquals(setOf("SØKNAD"), it["egenskaper"].map(JsonNode::asText).toSet())
            assertEquals(behandlingId, UUID.fromString(it["behandlingId"]?.asText()))
            assertEquals(null, it["saksbehandler"])
            assertEquals(null, it["beslutter"])
        }
    }

    @Test
    fun `publiserer OppgaveOppdatert med forventet format`() {
        // Given:
        val oppgaveId = nextLong()
        val behandlingId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val oppgave = Oppgave.nyOppgave(
            id = oppgaveId,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = UUID.randomUUID(),
            hendelseId = hendelseId,
            kanAvvises = true,
            egenskaper = setOf(Egenskap.SØKNAD),
            totrinnsvurdering = Totrinnsvurdering(
                vedtaksperiodeId = vedtaksperiodeId,
                erRetur = false,
                saksbehandler = null,
                beslutter = null,
                utbetalingId = null,
                opprettet = LocalDateTime.now(),
                oppdatert = null
            )
        )

        val saksbehandler = lagSaksbehandler()
        oppgave.forsøkTildeling(saksbehandler)
        oppgave.sendTilBeslutter(saksbehandler)

        val beslutter = lagSaksbehandler()
        oppgave.sendIRetur(beslutter)

        // When:
        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = OppgaveOppdatert(oppgave = oppgave),
            årsak = årsak,
        )

        // Then:
        assertEquals(1, testRapid.inspektør.size)
        testRapid.inspektør.message(index = 0).let {
            assertEquals("oppgave_oppdatert", it["@event_name"].asText())

            it["@forårsaket_av"]?.let { forårsaketAvObject ->
                assertEquals(hendelseId, UUID.fromString(forårsaketAvObject["id"]?.asText()))
            } ?: fail("Mangler @forårsaket_av")

            assertEquals(hendelseId, UUID.fromString(it["hendelseId"]?.asText()))
            assertEquals(fødselsnummer, it["fødselsnummer"].asText())
            assertEquals(oppgaveId, it["oppgaveId"]?.longValue())
            assertEquals(Oppgavestatus.AvventerSaksbehandler, enumValueOf<Oppgavestatus>(it["tilstand"].asText()))
            assertEquals(setOf("SØKNAD", "RETUR"), it["egenskaper"].map(JsonNode::asText).toSet())
            assertEquals(behandlingId, UUID.fromString(it["behandlingId"]?.asText()))

            it["saksbehandler"]?.let { saksbehandlerObject ->
                assertEquals(saksbehandler.epostadresse, saksbehandlerObject["epostadresse"]?.asText())
                assertEquals(saksbehandler.oid, UUID.fromString(saksbehandlerObject["oid"]?.asText()))
            } ?: fail("Mangler saksbehandler")

            it["beslutter"]?.let { beslutterObject ->
                assertEquals(beslutter.epostadresse, beslutterObject["epostadresse"]?.asText())
                assertEquals(beslutter.oid, UUID.fromString(beslutterObject["oid"]?.asText()))
            } ?: fail("Mangler beslutter")
        }
    }

}
