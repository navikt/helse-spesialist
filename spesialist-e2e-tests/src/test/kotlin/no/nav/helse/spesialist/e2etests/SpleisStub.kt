package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.test.TestPerson
import java.util.UUID

class SpleisStub(
    private val testPerson: TestPerson,
    private val rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val meldingsendere = mutableMapOf<UUID, SpleisTestMeldingPubliserer>()

    fun simulerFremTilOgMedGodkjenningsbehov(vedtaksperiodeId: UUID) {
        simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId)
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId)
    }

    fun simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = SpleisTestMeldingPubliserer(
            testPerson = testPerson,
            rapidsConnection = rapidsConnection,
            vedtaksperiodeId = vedtaksperiodeId,
        )
        meldingsendere[vedtaksperiodeId] = spleisTestMeldingPubliserer
        spleisTestMeldingPubliserer.simulerPublisertSendtSøknadNavMelding()
        spleisTestMeldingPubliserer.simulerPublisertBehandlingOpprettetMelding()
        spleisTestMeldingPubliserer.simulerPublisertVedtaksperiodeNyUtbetalingMelding()
    }

    fun simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertUtbetalingEndretMelding()
        spleisTestMeldingPubliserer.simulerPublisertVedtaksperiodeEndretMelding()
        spleisTestMeldingPubliserer.simulerPublisertGodkjenningsbehovMelding()
    }

    fun simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder: List<String>, vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder)
    }

    fun håndterUtbetalingUtbetalt(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertUtbetalingEndretTilUtbetaltMelding()
    }

    fun håndterAvsluttetMedVedtak(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertAvsluttetMedVedtakMelding()
    }

    fun simulerPublisertGosysOppgaveEndretMelding(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertGosysOppgaveEndretMelding()
    }

    fun registerOn(rapidsConnection: RapidsConnection) {
            River(rapidsConnection)
                .precondition(::precondition)
                .register(this)
    }

    private fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("Godkjenning"))
        jsonMessage.requireKey("@løsning")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val jsonNode = objectMapper.readTree(packet.toJson())
        val vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText())
        val godkjent = jsonNode["@løsning"]["Godkjenning"]["godkjent"].asBoolean()
        if (godkjent) {
            håndterUtbetalingUtbetalt(vedtaksperiodeId)
            håndterAvsluttetMedVedtak(vedtaksperiodeId)
        } else {
            logg.warn("Mottok godkjent = false i løsning på Godkjenning, håndterer ikke dette per nå")
        }
    }
}
