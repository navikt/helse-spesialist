package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.Meldingmottaker
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.spesialist.kafka.medRivers
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VedtaksperiodeReberegnetRiverTest {
    // sendt videre = "tatt inn," altså at preconditions og validations i riveren "aksepterer" meldingen
    var meldingenBleSendtVidere = false

    val meldingmottaker = object : Meldingmottaker {
        override fun mottaMelding(melding: Personmelding, kontekstbasertPubliserer: MeldingPubliserer) {
            meldingenBleSendtVidere = true
        }
    }

    @ParameterizedTest
    @CsvSource(
        "AVVENTER_GODKJENNING, AVVENTER_HISTORIKK",
        "SELVSTENDIG_AVVENTER_GODKJENNING, AVVENTER_HISTORIKK",
    )
    fun `riveren tar inn aktuelle meldinger`(forrigeTilstand: String, gjeldendeTilstand: String) {
        sendMelding(forrigeTilstand, gjeldendeTilstand)
        assertTrue { meldingenBleSendtVidere }
    }

    @ParameterizedTest
    @CsvSource(
        "TIL_UTBETALING, UTBETALING_FEILET",
        "AVVENTER_GODKJENNING_REVURDERING, TIL_INFOTRYGD",
        "AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING",
        "AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET",
        "SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING, SELVSTENDIG_TIL_UTBETALING",
        "TIL_INFOTRYGD, AVVENTER_SIMULERING",
        "TIL_UTBETALING, AVVENTER_GODKJENNING",
        "AVSLUTTET, AVVENTER_GODKJENNING",
    )
    fun `riveren tar ikke inn ikke-aktuelle meldinger`(forrigeTilstand: String, gjeldendeTilstand: String) {
        sendMelding(forrigeTilstand, gjeldendeTilstand)
        assertFalse { meldingenBleSendtVidere }
    }

    @Test
    fun `riveren tar ikke inn meldinger med ukjent forrige-tilstand (på godt og vondt)`() {
        sendMelding("UKJENT_TILSTAND", "AVVENTER_HISTORIKK")
        assertFalse(meldingenBleSendtVidere)
    }

    @Test
    fun `riveren tar inn meldinger med ukjent gjeldende-tilstand`() {
        sendMelding("AVVENTER_GODKJENNING_REVURDERING", "UKJENT_TILSTAND")
        assertTrue(meldingenBleSendtVidere)
    }

    private fun sendMelding(forrigeTilstand: String, gjeldendeTilstand: String) {
        TestRapid().medRivers(VedtaksperiodeReberegnetRiver(meldingmottaker)).sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeEndret(
                id = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fødselsnummer",
                forrigeTilstand = forrigeTilstand,
                gjeldendeTilstand = gjeldendeTilstand
            )
        )
    }
}
