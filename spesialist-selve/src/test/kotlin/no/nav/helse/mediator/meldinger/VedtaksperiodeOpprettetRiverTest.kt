package no.nav.helse.mediator.meldinger

import io.mockk.called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeOpprettetRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)

    init {
        VedtaksperiodeOpprettetRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `behandler vedtaksperiode_opprettet`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeOpprettet(aktørId = AKTØR, fødselsnummer = FØDSELSNUMMER)
        )
        verify(exactly = 1) {
            mediator.vedtaksperiodeOpprettet(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `ignorerer organisasjonsnummer ARBEIDSLEDIG uten å tryne`() {
        rapid.sendTestMessage(
            """
                {
                  "@event_name": "vedtaksperiode_opprettet",
                  "@id": "5cce02b3-5622-4a7d-a149-970b2c285475",
                  "@opprettet": "2023-11-23T22:45:37.604553",
                  "vedtaksperiodeId": "b42fcca4-5242-4526-8d5b-86c2cd1b29c6",
                  "fødselsnummer": "12020052345",
                  "aktørId": "999999999",
                  "organisasjonsnummer": "ARBEIDSLEDIG",
                  "@forårsaket_av": {
                    "id": "2ddae041-ecdf-4a7e-915a-28404ccc51a7",
                    "opprettet": "2018-02-01T00:00:00"
                  },
                  "fom": "2018-01-01",
                  "tom": "2018-01-31",
                  "skjæringstidspunkt": "2018-01-01",
                  "system_read_count": 0,
                  "system_participating_services": [
                    {
                      "id": "5cce02b3-5622-4a7d-a149-970b2c285475",
                      "time": "2023-11-23T22:45:37.635488"
                    }
                  ]
                }
            """.trimIndent()
        )
        verify { mediator wasNot called }
    }
}
