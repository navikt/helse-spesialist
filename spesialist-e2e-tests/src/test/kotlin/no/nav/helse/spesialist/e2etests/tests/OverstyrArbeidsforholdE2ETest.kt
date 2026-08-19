package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OverstyrArbeidsforholdE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler overstyrer arbeidsforhold`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerOverstyrerArbeidsforhold()
        }

        // Then:
        val melding =
            meldinger().findLast { it["@event_name"].asString() == "overstyr_arbeidsforhold" }
                ?: error("Forventet å finne overstyr_arbeidsforhold i meldingslogg")

        assertEquals(testContext.person.fødselsnummer, melding["fødselsnummer"].asString())
        assertEquals(saksbehandler.id.value, melding["saksbehandlerOid"].asUUID())
        assertEquals(
            førsteVedtaksperiode().skjæringstidspunkt.toString(),
            melding["skjæringstidspunkt"].asString(),
        )
        val overstyrtArbeidsforhold = melding["overstyrteArbeidsforhold"].single()
        assertEquals(
            testContext.arbeidsgiver.organisasjonsnummer,
            overstyrtArbeidsforhold["orgnummer"].asString(),
        )
        assertEquals(true, overstyrtArbeidsforhold["deaktivert"].asBoolean())
    }
}
