package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvsluttetUtenVedtakRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `avsluttet med vedtak blir prosessert riktig`() {
        // Given:
        val person = lagPerson()
            .also(sessionContext.personRepository::lagre)

        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)

        val behandling = lagBehandling(
            vedtaksperiodeId = vedtaksperiode.id,
            tilstand = Behandling.Tilstand.VidereBehandlingAvklares,
            utbetalingId = null,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
        ).also(sessionContext.behandlingRepository::lagre)

        // When:
        testRapid.sendTestMessage(avsluttetUtenVedtakMelding(person, vedtaksperiode, behandling))

        // Then:
        assertEquals(0, testRapid.publiserteMeldingerUtenGenererteFelter().size)
        assertEquals(
            Behandling.Tilstand.AvsluttetUtenVedtak,
            sessionContext.behandlingRepository.finn(behandling.id)?.tilstand
        )
    }

    @Language("JSON")
    private fun avsluttetUtenVedtakMelding(
        person: Person,
        vedtaksperiode: Vedtaksperiode,
        behandling: Behandling,
    ) =
        """
    {
      "@event_name": "avsluttet_uten_vedtak",
      "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
      "yrkesaktivitetstype": "ARBEIDSTAKER",
      "vedtaksperiodeId": "${vedtaksperiode.id.value}",
      "behandlingId": "${behandling.spleisBehandlingId!!.value}",
      "fom": "${behandling.fom}",
      "tom": "${behandling.tom}",
      "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
      "hendelser": [
        "d59986f7-24c1-4b7b-858b-3a9421eb0d04",
        "46cd1183-3c13-435e-a4a1-4321797159f7"
      ],
      "avsluttetTidspunkt": "2025-08-05T12:35:30.405991344",
      "@id": "e1a3a479-b485-48fb-86ed-8a73714c23ed",
      "@opprettet": "2025-08-05T12:35:30.406397224",
      "system_read_count": 1,
      "system_participating_services": [
        {
          "id": "e1a3a479-b485-48fb-86ed-8a73714c23ed",
          "time": "2025-08-05T12:35:30.406397224",
          "service": "helse-spleis",
          "instance": "helse-spleis-78ccf7d6c9-q67dw",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.05-10.27-ce63981"
        },
        {
          "id": "e1a3a479-b485-48fb-86ed-8a73714c23ed",
          "time": "2025-08-05T12:35:30.964091493",
          "service": "spesialist",
          "instance": "spesialist-58c64b76cd-hbl8z",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.05-08.17-593599a"
        }
      ],
      "fødselsnummer": "${person.id.value}",
      "@forårsaket_av": {
        "id": "46cd1183-3c13-435e-a4a1-4321797159f7",
        "opprettet": "2024-05-01T00:00",
        "event_name": "inntektsmelding"
      }
    }
""".trimIndent()
}
