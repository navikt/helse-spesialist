package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.helse.kafka.NyeVarslerRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class NyeVarslerRiverTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val ORGNR = "123456789"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(NyeVarslerRiver(mediator))

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser aktivitetslogg_ny_aktivitet`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagAktivitetsloggNyAktivitet(FNR, HENDELSE, VEDTAKSPERIODE, ORGNR))
        verify(exactly = 1) { mediator.mottaMelding(any<NyeVarsler>(), any()) }
    }

    @Test
    fun `leser nye_varsler`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagNyeVarsler(FNR, HENDELSE, VEDTAKSPERIODE, ORGNR))
        verify(exactly = 1) { mediator.mottaMelding(any<NyeVarsler>(), any()) }
    }

    @Test
    fun `leser ikke meldinger som ikke inneholder varsler`() {
        val riverSpy = spyk(NyeVarslerRiver(mediator))
        val rapid = TestRapid().medRivers(riverSpy)

        rapid.sendTestMessage(melding(FNR, "VARSEL", "INFO", "VARSEL"))
        verify(exactly = 1) { riverSpy.onPacket(any(), any(), any(), any()) }
        clearMocks(riverSpy)

        rapid.sendTestMessage(melding(FNR, "INFO", "INFO"))
        verify(exactly = 0) { riverSpy.onPacket(any(), any(), any(), any()) }
        clearMocks(riverSpy)

        rapid.sendTestMessage(melding(FNR, "VARSEL"))
        verify(exactly = 1) { riverSpy.onPacket(any(), any(), any(), any()) }
    }

    private fun melding(fødselsnummer: String, vararg nivåer: String) = mapOf(
        "@event_name" to "aktivitetslogg_ny_aktivitet",
        "@id" to "${UUID.randomUUID()}",
        "@opprettet" to "${LocalDateTime.now()}",
        "fødselsnummer" to fødselsnummer,
        "aktiviteter" to nivåer.map { nivå ->
            buildMap {
                put("id", "${UUID.randomUUID()}")
                put("nivå", nivå)
                if (nivå == "VARSEL") put("varselkode", "123")
                put("melding", "en melding")
                val vedtaksperiodeId = UUID.randomUUID()
                put(
                    "kontekster", listOf(
                        mapOf(
                            "konteksttype" to "Vedtaksperiode",
                            "kontekstmap" to mapOf("vedtaksperiodeId" to "$vedtaksperiodeId")
                        )
                    )
                )
                put("tidsstempel", "${LocalDateTime.now()}")
            }
        }
    ).let(objectMapper::writeValueAsString)
}
