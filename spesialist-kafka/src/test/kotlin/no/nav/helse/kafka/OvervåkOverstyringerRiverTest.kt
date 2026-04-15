package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.medRivers
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class OvervåkOverstyringerRiverTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    val sessionFactory = integrationTestFixture.sessionFactory
    private val sessionContext = sessionFactory.sessionContext

    init {
        testRapid.medRivers(OvervåkOverstyringerRiver(sessionFactory))
    }

    @Test
    fun `overstyringer i blå perioder gir innslag i venter-tabellen`() {
        // Given
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(
            vedtaksperiodeId = vedtaksperiode.id,
            tilstand = Behandling.Tilstand.VidereBehandlingAvklares,
        ).also(sessionContext.behandlingRepository::lagre)
        val meldingId = UUID.randomUUID()

        // When
        testRapid.sendTestMessage(overstyrInntektOgRefusjon(meldingId, vedtaksperiode.id.value, person.id))

        // Then
        assertEquals(
            person.id,
            sessionContext.venterPåKvitteringForOverstyringRepository.finn(MeldingId(meldingId))?.identitetsnummer
        )
    }

    @Test
    fun `gjør ikke noe med overstyringer i oransje perioder`() {
        // Given
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(
            vedtaksperiodeId = vedtaksperiode.id,
            tilstand = Behandling.Tilstand.KlarTilBehandling,
        ).also(sessionContext.behandlingRepository::lagre)
        val meldingId = UUID.randomUUID()

        // When
        testRapid.sendTestMessage(overstyrInntektOgRefusjon(meldingId, vedtaksperiode.id.value, person.id))

        // Then
        assertNull(
            sessionContext.venterPåKvitteringForOverstyringRepository.finn(MeldingId(meldingId))?.identitetsnummer
        )
    }

    @Language("JSON")
    private fun overstyrInntektOgRefusjon(meldingId: UUID, vedtaksperiodeId: UUID, fødselsnummer: Identitetsnummer) =
        """
        {
          "@event_name": "overstyr_tidslinje",
          "fødselsnummer": "${fødselsnummer.value}",
          "@id": "$meldingId",
          "aktørId": "2312706686680",
          "organisasjonsnummer": "907670201",
          "vedtaksperiodeId" : "$vedtaksperiodeId",
          "dager": [
            {
              "dato": "2025-02-04",
              "type": "Feriedag",
              "fraType": "Sykedag",
              "grad": null,
              "fraGrad": 100
            },
            {
              "dato": "2025-02-05",
              "type": "Feriedag",
              "fraType": "Sykedag",
              "grad": null,
              "fraGrad": 100
            }
          ],
          "@opprettet": "2026-04-15T09:40:22.647949648",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "ac5b160a-ad47-4dd4-aee6-684aef730ebb",
              "time": "2026-04-15T09:40:22.647949648",
              "service": "spesialist",
              "instance": "spesialist-f44f8ffb8-xtx9k",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2026.04.14-12.59-3e4c3a9"
            }
          ]
        }
        """.trimIndent()
}
