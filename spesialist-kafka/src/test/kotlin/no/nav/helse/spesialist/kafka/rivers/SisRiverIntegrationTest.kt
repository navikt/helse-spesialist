package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import org.intellij.lang.annotations.Language
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test

class SisRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val testFixture = IntegrationTestFixture(testRapid)

    @Test
    fun `Tar imot behandlingstatusmelding`() {
        // Given
        val vedtaksperiodeId = UUID.randomUUID()
        // When
        testRapid.sendTestMessage(behandlingstatusmelding(vedtaksperiodeId), vedtaksperiodeId.toString())
        // Then
    }

    @Language("JSON")
    private fun behandlingstatusmelding(vedtaksperiodeId: UUID = UUID.randomUUID()): String {
        return """
            {
                "vedtaksperiodeId": "$vedtaksperiodeId",
                "behandlingId": "${UUID.randomUUID()}",
                "tidspunkt": "${OffsetDateTime.now(ZoneId.of("Europe/Oslo"))}",
                "status": "OPPRETTET",
                "eksterneSøknadIder": ["${UUID.randomUUID()}"]
            }
        """.trimIndent()
    }
}