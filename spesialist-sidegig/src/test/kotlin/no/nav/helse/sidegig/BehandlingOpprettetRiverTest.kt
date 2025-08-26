package no.nav.helse.sidegig

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class BehandlingOpprettetRiverTest {
    private val testRapid = TestRapid()

    @Test
    fun `leser behandling_opprettet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val fom = LocalDate.of(2018, 1, 1)
        val tom = LocalDate.of(2018, 1, 31)
        val opprettet = LocalDateTime.now()
        BehandlingOpprettetRiver(testRapid, dao)

        testRapid.sendTestMessage(
            melding(
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                opprettet = opprettet
            )
        )

        val behandlingen = dao.behandlinger[0]
        assertEquals(1, dao.behandlinger.size)
        assertEquals(vedtaksperiodeId, behandlingen.vedtaksperiodeId)
        assertEquals(behandlingId, behandlingen.behandlingId)
        assertEquals(fom, behandlingen.fom)
        assertEquals(tom, behandlingen.tom)
        assertEquals(fom, behandlingen.skjæringstidspunkt)
        assertEquals(opprettet, behandlingen.opprettet)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ARBEIDSLEDIG", "SELVSTENDIG", "FRILANS"])
    fun `leser ikke behandlinger med yrkesaktivitetstype=`(yrkesaktivitetstype: String) {
        BehandlingOpprettetRiver(testRapid, dao)

        testRapid.sendTestMessage(melding(yrkesaktivitetstype = yrkesaktivitetstype,))
        assertEquals(0, dao.behandlinger.size)
    }

    @Language("JSON")
    private fun melding(
        organisasjonsnummer: String = "987654321",
        yrkesaktivitetstype: String = "ARBEIDSTAKER",
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = """{
  "@event_name": "behandling_opprettet",
  "organisasjonsnummer": "$organisasjonsnummer",
  "yrkesaktivitetstype": "$yrkesaktivitetstype",
  "vedtaksperiodeId": "$vedtaksperiodeId",
  "behandlingId": "$behandlingId",
  "søknadIder": [
    "eab4ec39-0219-4f53-a7f1-7d10f3944df5"
  ],
  "type": "Søknad",
  "fom": "$fom",
  "tom": "$tom",
  "kilde": {
    "meldingsreferanseId": "5c129838-2e84-41c9-a98c-f0ccd73ac296",
    "innsendt": "2018-01-31T00:00:00.000",
    "registrert": "2018-01-31T00:00:00.000",
    "avsender": "SYKMELDT"
  },
  "@id": "28140d37-a687-4e9b-8278-2435fe2a5051",
  "@opprettet": "$opprettet",
  "fødselsnummer": "12345678910"
}
""".trimIndent()

    private val dao = object: BehandlingDao {
        val behandlinger = mutableListOf<Behandling>()
        override fun lagreBehandling(behandling: Behandling) {
            behandlinger.add(behandling)
        }
    }
}
