package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InfotrygdE2ETest : AbstractE2EIntegrationTest() {

    @Test
    fun `filtrerer ut sanksjon fra utbetalingshistorikk`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        hentInfotrygdutbetalingerBehovLøser.historikk =
            listOf(
                mapOf(
                    "fom" to "2018-01-01",
                    "tom" to "2018-01-31",
                    "dagsats" to 1000.0,
                    "grad" to "100",
                    "typetekst" to "ArbRef",
                    "organisasjonsnummer" to organisasjonsnummer()
                ),
                mapOf(
                    "fom" to "2017-01-01",
                    "tom" to null,
                    "dagsats" to 0.0,
                    "grad" to "",
                    "typetekst" to "Sanksjon",
                    "organisasjonsnummer" to "0"
                )
            )

        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            assertEquals(
                objectMapper.readTree(
                    objectMapper.writeValueAsString(
                        listOf(
                            mapOf(
                                "fom" to "2018-01-01",
                                "tom" to "2018-01-31",
                                "dagsats" to 1000.0,
                                "grad" to "100",
                                "typetekst" to "ArbRef",
                                "organisasjonsnummer" to organisasjonsnummer(),
                                "__typename" to "Infotrygdutbetaling"
                            )
                        )
                    )
                ),
                person["infotrygdutbetalinger"],
            )
        }
    }
}
