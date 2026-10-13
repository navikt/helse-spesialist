package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertNull

class SaksbehandlerhandlingRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `lagrer saksbehandlerhandling uten totrinnsvurdering`() {
        // Given:
        val identitetsnummer = lagPerson().id
        val handlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val tidspunkt = Instant.parse("2026-10-12T12:00:00Z")

        // When:
        testRapid.sendTestMessage(
            saksbehandlerhandlingMelding(
                identitetsnummer = identitetsnummer,
                handlingId = handlingId,
                tidspunkt = tidspunkt,
                kreverTotrinnsvurdering = false,
                skjæringstidspunkt = LocalDate.parse("2026-01-01"),
                behandlingId = behandlingId,
            ),
        )

        // Then:
        val handling = sessionContext.saksbehandlerhandlingRepository.alle().single()
        assertEquals(identitetsnummer, handling.identitetsnummer)
        assertEquals(handlingId, handling.handlingId)
        assertEquals(HandlingType.MANUELT_VURDERT_OPPTJENING, handling.handlingType)
        assertEquals("A123456", handling.saksbehandlerIdent)
        assertEquals(tidspunkt, handling.tidspunkt)
        assertEquals(LocalDate.parse("2026-01-01"), handling.skjæringstidspunkt)
        assertEquals(SpleisBehandlingId(behandlingId), handling.behandlingId)
        assertNull(handling.totrinnsvurderingId)
        assertEquals(0, sessionContext.totrinnsvurderingRepository.alle().size)
    }

    @Test
    fun `oppretter totrinnsvurdering når handlingen krever det og ingen finnes`() {
        // Given:
        val identitetsnummer = lagPerson().id

        // When:
        testRapid.sendTestMessage(
            saksbehandlerhandlingMelding(identitetsnummer = identitetsnummer, kreverTotrinnsvurdering = true),
        )

        // Then:
        val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.alle().single()
        assertEquals(identitetsnummer.value, totrinnsvurdering.fødselsnummer)
        val handling = sessionContext.saksbehandlerhandlingRepository.alle().single()
        assertEquals(totrinnsvurdering.id().value, handling.totrinnsvurderingId?.value)
    }

    @Test
    fun `gjenbruker aktiv totrinnsvurdering når handlingen krever det`() {
        // Given:
        val identitetsnummer = lagPerson().id
        val eksisterende =
            Totrinnsvurdering
                .ny(fødselsnummer = identitetsnummer.value)
                .also(sessionContext.totrinnsvurderingRepository::lagre)

        // When:
        testRapid.sendTestMessage(
            saksbehandlerhandlingMelding(identitetsnummer = identitetsnummer, kreverTotrinnsvurdering = true),
        )

        // Then:
        assertEquals(1, sessionContext.totrinnsvurderingRepository.alle().size)
        val handling = sessionContext.saksbehandlerhandlingRepository.alle().single()
        assertEquals(eksisterende.id().value, handling.totrinnsvurderingId?.value)
    }

    @Test
    fun `lagrer handling uten valgfrie felter`() {
        // Given:
        val identitetsnummer = lagPerson().id

        // When:
        testRapid.sendTestMessage(
            saksbehandlerhandlingMelding(
                identitetsnummer = identitetsnummer,
                kreverTotrinnsvurdering = false,
                skjæringstidspunkt = null,
                behandlingId = null,
            ),
        )

        // Then:
        val handling = sessionContext.saksbehandlerhandlingRepository.alle().single()
        assertNull(handling.skjæringstidspunkt)
        assertNull(handling.behandlingId)
    }

    @Test
    fun `feiler på melding uten kreverTotrinnsvurdering`() {
        // Given:
        val identitetsnummer = lagPerson().id

        // When:
        assertThrows<IllegalStateException> {
            testRapid.sendTestMessage(
                saksbehandlerhandlingMelding(identitetsnummer = identitetsnummer, kreverTotrinnsvurdering = null),
            )
        }

        // Then:
        assertEquals(0, sessionContext.saksbehandlerhandlingRepository.alle().size)
    }

    @Test
    fun `feiler på ukjent handlingType`() {
        // Given:
        val identitetsnummer = lagPerson().id

        // When:
        assertThrows<IllegalStateException> {
            testRapid.sendTestMessage(
                saksbehandlerhandlingMelding(
                    identitetsnummer = identitetsnummer,
                    handlingType = "UKJENT_HANDLING",
                    kreverTotrinnsvurdering = false,
                ),
            )
        }

        // Then:
        assertEquals(0, sessionContext.saksbehandlerhandlingRepository.alle().size)
    }

    private fun saksbehandlerhandlingMelding(
        identitetsnummer: Identitetsnummer,
        handlingId: UUID = UUID.randomUUID(),
        handlingType: String = "MANUELT_VURDERT_OPPTJENING",
        tidspunkt: Instant = Instant.now(),
        kreverTotrinnsvurdering: Boolean?,
        skjæringstidspunkt: LocalDate? = null,
        behandlingId: UUID? = null,
    ): String {
        val valgfrieFelter =
            listOfNotNull(
                kreverTotrinnsvurdering?.let { "\"kreverTotrinnsvurdering\": $it" },
                skjæringstidspunkt?.let { "\"skjæringstidspunkt\": \"$it\"" },
                behandlingId?.let { "\"behandlingId\": \"$it\"" },
            ).joinToString(separator = "") { ",\n  $it" }

        @Language("JSON")
        val melding =
            """
            {
              "@event_name": "saksbehandlerhandling",
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "2026-10-12T12:00:00",
              "fødselsnummer": "${identitetsnummer.value}",
              "saksbehandlerIdent": "A123456",
              "handlingId": "$handlingId",
              "handlingType": "$handlingType",
              "tidspunkt": "$tidspunkt"$valgfrieFelter
            }
            """.trimIndent()
        return melding
    }
}
