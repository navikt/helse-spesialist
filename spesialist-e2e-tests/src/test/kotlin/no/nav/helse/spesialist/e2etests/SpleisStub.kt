package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spesialist.test.TestPerson
import java.util.UUID

class SpleisStub(
    private val testPerson: TestPerson,
    private val rapidsConnection: RapidsConnection
) {
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
        spleisTestMeldingPubliserer.håndterUtbetalingUtbetalt()
    }

    fun håndterAvsluttetMedVedtak(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.håndterAvsluttetMedVedtak()
    }

    fun simulerPublisertGosysOppgaveEndretMelding(vedtaksperiodeId: UUID) {
        val spleisTestMeldingPubliserer = meldingsendere[vedtaksperiodeId]
            ?: error("Fant ikke spleisTestMeldingPubliserer for vedtaksperiodeId: $vedtaksperiodeId")
        spleisTestMeldingPubliserer.simulerPublisertGosysOppgaveEndretMelding()
    }
}