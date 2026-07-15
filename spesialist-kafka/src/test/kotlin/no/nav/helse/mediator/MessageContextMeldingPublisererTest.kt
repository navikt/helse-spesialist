package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.modell.melding.HentDokument
import no.nav.helse.modell.melding.OppgaveOppdatert
import no.nav.helse.modell.melding.OppgaveOpprettet
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Inntektsforhold
import no.nav.helse.spesialist.domain.oppgave.Mottaker
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.Oppgavetype
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.TestRapidHelpers.meldinger
import no.nav.helse.spesialist.kafka.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.convertValue
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
    fun `publiserer hendelse med forventet format`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val periodetype = "FORLENGELSE"
        val utgåendeHendelse =
            VedtaksperiodeGodkjentAutomatisk(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                periodetype = periodetype,
            )

        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = utgåendeHendelse,
            årsak = årsak,
        )

        assertEquals("vedtaksperiode_godkjent", testRapid.inspektør.field(0, "@event_name").asString())
        assertEquals(fødselsnummer, testRapid.inspektør.field(0, "fødselsnummer").asString())
        assertEquals(vedtaksperiodeId.toString(), testRapid.inspektør.field(0, "vedtaksperiodeId").asString())
        assertEquals(behandlingId.toString(), testRapid.inspektør.field(0, "behandlingId").asString())
        assertEquals(periodetype, testRapid.inspektør.field(0, "periodetype").asString())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asString()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asString()) }
    }

    @Test
    fun `publiserer KommandokjedeEndretEvent med forventet format`() {
        val event =
            KommandokjedeEndretEvent.Avbrutt(
                commandContextId = contextId,
                hendelseId = hendelseId,
            )

        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            event = event,
            hendelseNavn = årsak,
        )

        assertEquals("kommandokjede_avbrutt", testRapid.inspektør.field(0, "@event_name").asString())
        assertEquals(contextId.toString(), testRapid.inspektør.field(0, "commandContextId").asString())
        assertEquals(hendelseId.toString(), testRapid.inspektør.field(0, "meldingId").asString())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asString()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asString()) }
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
            assertEquals("hent-dokument", it["@event_name"].asString())
            assertEquals(dokumentId, UUID.fromString(it["dokumentId"].asString()))
            assertEquals(dokumentType, it["dokumentType"].asString())
        }
    }

    @Test
    fun `publiserer OppgaveOpprettet med forventet format`() {
        // Given:
        val oppgaveId = nextLong()
        val behandlingId = lagSpleisBehandlingId()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId = lagVedtaksperiodeId()

        val oppgave =
            Oppgave.ny(
                id = oppgaveId,
                førsteOpprettet = null,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = UUID.randomUUID(),
                hendelseId = hendelseId,
                kanAvvises = true,
                egenskaper = setOf(Egenskap.SØKNAD),
                mottaker = Mottaker.UtbetalingTilArbeidsgiver,
                oppgavetype = Oppgavetype.Søknad,
                inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
                inntektsforhold = Inntektsforhold.Arbeidstaker,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
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
            assertEquals("oppgave_opprettet", it["@event_name"].asString())

            it["@forårsaket_av"]?.let { forårsaketAvObject ->
                assertEquals(hendelseId, UUID.fromString(forårsaketAvObject["id"]?.asString()))
            } ?: fail("Mangler @forårsaket_av")

            assertEquals(hendelseId, UUID.fromString(it["hendelseId"]?.asString()))
            assertEquals(fødselsnummer, it["fødselsnummer"].asString())
            assertEquals(oppgaveId, it["oppgaveId"]?.longValue())
            assertEquals(Oppgavestatus.AvventerSaksbehandler, enumValueOf<Oppgavestatus>(it["tilstand"].asString()))
            assertEquals(setOf("SØKNAD"), it["egenskaper"].toList().map(JsonNode::asString).toSet())
            assertEquals(behandlingId.value, UUID.fromString(it["behandlingId"]?.asString()))
            assertEquals(null, it["saksbehandler"])
            assertEquals(null, it["beslutter"])
        }
    }

    @Test
    fun `publiserer OppgaveOppdatert med forventet format`() {
        // Given:
        val oppgaveId = nextLong()
        val behandlingId = lagSpleisBehandlingId()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId = lagVedtaksperiodeId()

        val oppgave =
            Oppgave.ny(
                id = oppgaveId,
                førsteOpprettet = null,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = UUID.randomUUID(),
                hendelseId = hendelseId,
                kanAvvises = true,
                egenskaper = setOf(Egenskap.SØKNAD),
                mottaker = Mottaker.UtbetalingTilArbeidsgiver,
                oppgavetype = Oppgavetype.Søknad,
                inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
                inntektsforhold = Inntektsforhold.Arbeidstaker,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            )

        val saksbehandler = lagSaksbehandler()
        val beslutter = lagSaksbehandler()
        oppgave.tildelTil(
            saksbehandler = saksbehandler,
            brukerroller = emptySet(),
        )
        oppgave.sendTilBeslutter(beslutter)

        oppgave.sendIRetur(saksbehandler)

        // When:
        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = OppgaveOppdatert(oppgave = oppgave),
            årsak = årsak,
        )

        // Then:
        assertEquals(1, testRapid.inspektør.size)
        testRapid.inspektør.message(index = 0).let {
            assertEquals("oppgave_oppdatert", it["@event_name"].asString())

            it["@forårsaket_av"]?.let { forårsaketAvObject ->
                assertEquals(hendelseId, UUID.fromString(forårsaketAvObject["id"]?.asString()))
            } ?: fail("Mangler @forårsaket_av")

            assertEquals(hendelseId, UUID.fromString(it["hendelseId"]?.asString()))
            assertEquals(fødselsnummer, it["fødselsnummer"].asString())
            assertEquals(oppgaveId, it["oppgaveId"]?.longValue())
            assertEquals(Oppgavestatus.AvventerSaksbehandler, enumValueOf<Oppgavestatus>(it["tilstand"].asString()))
            assertEquals(setOf("SØKNAD", "RETUR"), it["egenskaper"].toList().map(JsonNode::asString).toSet())
            assertEquals(behandlingId.value, UUID.fromString(it["behandlingId"]?.asString()))
            assertEquals(saksbehandler.id.value, it["saksbehandler"]?.asUUID())
        }
    }

    @Test
    fun `publiserer SubsumsjonEvent med forventet format`() {
        val id = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()

        val subsumsjonEvent =
            SubsumsjonEvent(
                id = id,
                fødselsnummer = fødselsnummer,
                paragraf = "EN PARAGRAF",
                ledd = "ET LEDD",
                bokstav = "EN BOKSTAV",
                lovverk = "folketrygdloven",
                lovverksversjon = "1970-01-01",
                utfall = "VILKAR_BEREGNET",
                input = mapOf("foo" to "bar"),
                output = mapOf("foo" to "bar"),
                sporing = mapOf("identifikator" to listOf("EN ID")),
                tidsstempel = tidsstempel,
                kilde = "KILDE",
            )

        meldingPubliserer.publiser(fødselsnummer, subsumsjonEvent, "versjonAvKode")

        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()
        val subsumsjon = melding.path("subsumsjon")

        assertEquals("subsumsjon", melding["@event_name"].asString())
        assertNotNull(melding["@id"].asUUID())
        assertNotNull(melding["@opprettet"].asLocalDateTime())
        assertEquals(id, subsumsjon["id"].asUUID())
        assertEquals(fødselsnummer, subsumsjon["fodselsnummer"].asString())
        assertEquals("versjonAvKode", subsumsjon["versjonAvKode"].asString())
        assertEquals("1.0.0", subsumsjon["versjon"].asString())
        assertEquals("EN PARAGRAF", subsumsjon["paragraf"].asString())
        assertEquals("ET LEDD", subsumsjon["ledd"].asString())
        assertEquals("EN BOKSTAV", subsumsjon["bokstav"].asString())
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asString())
        assertEquals("1970-01-01", subsumsjon["lovverksversjon"].asString())
        assertEquals("VILKAR_BEREGNET", subsumsjon["utfall"].asString())
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]))
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(subsumsjon["output"]))
        assertEquals(tidsstempel, subsumsjon["tidsstempel"].asLocalDateTime())
        assertEquals("KILDE", subsumsjon["kilde"].asString())
    }

    @Test
    fun `publiserer SubsumsjonEvent uten ledd og bokstav med forventet format`() {
        val id = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()

        val subsumsjonEvent =
            SubsumsjonEvent(
                id = id,
                fødselsnummer = fødselsnummer,
                paragraf = "EN PARAGRAF",
                ledd = null,
                bokstav = null,
                lovverk = "folketrygdloven",
                lovverksversjon = "1970-01-01",
                utfall = "VILKAR_BEREGNET",
                input = mapOf("foo" to "bar"),
                output = mapOf("foo" to "bar"),
                sporing = mapOf("identifikator" to listOf("EN ID")),
                tidsstempel = tidsstempel,
                kilde = "KILDE",
            )

        meldingPubliserer.publiser(fødselsnummer, subsumsjonEvent, "versjonAvKode")

        val meldinger = testRapid.inspektør.meldinger()
        assertEquals(1, meldinger.size)
        val melding = meldinger.single()
        val subsumsjon = melding.path("subsumsjon")

        assertEquals("subsumsjon", melding["@event_name"].asString())
        assertNotNull(melding["@id"].asUUID())
        assertNotNull(melding["@opprettet"].asLocalDateTime())
        assertEquals(id, subsumsjon["id"].asUUID())
        assertEquals(fødselsnummer, subsumsjon["fodselsnummer"].asString())
        assertEquals("versjonAvKode", subsumsjon["versjonAvKode"].asString())
        assertEquals("1.0.0", subsumsjon["versjon"].asString())
        assertEquals("EN PARAGRAF", subsumsjon["paragraf"].asString())
        assertNull(subsumsjon["ledd"])
        assertNull(subsumsjon["bokstav"])
        assertEquals("folketrygdloven", subsumsjon["lovverk"].asString())
        assertEquals("1970-01-01", subsumsjon["lovverksversjon"].asString())
        assertEquals("VILKAR_BEREGNET", subsumsjon["utfall"].asString())
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(subsumsjon["input"]))
        assertEquals(mapOf("foo" to "bar"), objectMapper.convertValue<Map<String, Any>>(subsumsjon["output"]))
        assertEquals(tidsstempel, subsumsjon["tidsstempel"].asLocalDateTime())
        assertEquals("KILDE", subsumsjon["kilde"].asString())
    }
}
