package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class MessageContextMeldingPublisererTest {
    private val testRapid: TestRapid = TestRapid()
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val contextId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val fødselsnummer = lagFødselsnummer()

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
        val hendelseNavn = "JUnit"

        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            hendelse = utgåendeHendelse,
            årsak = hendelseNavn,
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
        val hendelseNavn = "JUnit"

        meldingPubliserer.publiser(
            event = event,
            hendelseNavn = hendelseNavn,
        )

        assertEquals("kommandokjede_avbrutt", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(contextId.toString(), testRapid.inspektør.field(0, "commandContextId").asText())
        assertEquals(hendelseId.toString(), testRapid.inspektør.field(0, "meldingId").asText())
        assertDoesNotThrow { UUID.fromString(testRapid.inspektør.field(0, "@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(testRapid.inspektør.field(0, "@opprettet").asText()) }
    }
}
