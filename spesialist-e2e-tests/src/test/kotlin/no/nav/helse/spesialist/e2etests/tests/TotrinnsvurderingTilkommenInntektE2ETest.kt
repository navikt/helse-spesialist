package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class TotrinnsvurderingTilkommenInntektE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ny tilkommen inntekt blir del av aktiv totrinnsvurdering`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = lagOrganisasjonsnummer(),
                periode = (2 jan 2021) tilOgMed (31 jan 2021),
                periodebeløp = BigDecimal("1111.11"),
                ekskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021),
                notatTilBeslutter = "notat"
            )

            // Then:
            assertEquals(1, tilkomneInntektskilder.size())
            tilkomneInntektskilder[0]["inntekter"].let { inntekter ->
                assertEquals(1, inntekter.size())
                assertEquals(true, inntekter[0]["erDelAvAktivTotrinnsvurdering"].asBoolean())
            }
        }
    }

    @Test
    fun `tilkommen inntekt regnes ikke som del av aktiv totrinnsvurdering etter at totrinnsvurderingen blir godkjent`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = lagOrganisasjonsnummer(),
                periode = (2 jan 2021) tilOgMed (31 jan 2021),
                periodebeløp = BigDecimal("1111.11"),
                ekskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021),
                notatTilBeslutter = "notat"
            )
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerSenderTilGodkjenning()
        }

        beslutterMedPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" beslutter
            saksbehandlerFatterVedtak()
        }

        // Then:
        medPersonISpeil {
            val tilkomneInntektskilder = tilkomneInntektskilder
            assertEquals(1, tilkomneInntektskilder.size())
            tilkomneInntektskilder[0]["inntekter"].let { inntekter ->
                assertEquals(1, inntekter.size())
                assertEquals(false, inntekter[0]["erDelAvAktivTotrinnsvurdering"].asBoolean())
            }
        }
    }
}
