package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyring
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeldingOmMeldingHåndtertRiverTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    val sessionFactory = integrationTestFixture.sessionFactory
    private val sessionContext = sessionFactory.sessionContext

    @Test
    fun `lager ikke opptegnelse for urelaterte kvitteringer`() {
        // Given
        val urelatertKvittering = kvittering(UUID.randomUUID(), "inntektsmelding")

        // When
        testRapid.sendTestMessage(urelatertKvittering)

        // Then
        assertEquals(0, sessionContext.opptegnelseRepository.alle().size)
    }

    @Test
    fun `lager ikke opptegnelse for kvitteringer for ikke-ventende overstyringer`() {
        // Given
        sessionContext.venterPåKvitteringForOverstyringRepository.lagre(
            VenterPåKvitteringForOverstyring.ny(UUID.randomUUID().let(::MeldingId), lagIdentitetsnummer())
        )
        val kvitteringForEnOverstyring = kvittering(UUID.randomUUID(), "overstyr_tidslinje")

        // When
        testRapid.sendTestMessage(kvitteringForEnOverstyring)

        // Then
        assertEquals(0, sessionContext.opptegnelseRepository.alle().size)
    }

    @Test
    fun `lager opptegnelse og sletter venter-rad ved kvittering for ventende overstyring`() {
        // Given
        val meldingId = UUID.randomUUID().let(::MeldingId)
        sessionContext.venterPåKvitteringForOverstyringRepository.lagre(
            VenterPåKvitteringForOverstyring.ny(meldingId, lagIdentitetsnummer())
        )
        val kvitteringForVentendeOverstyring = kvittering(meldingId.value, "overstyr_tidslinje")

        // When
        testRapid.sendTestMessage(kvitteringForVentendeOverstyring)

        // Then
        assertEquals(Opptegnelse.Type.PERSONDATA_OPPDATERT, sessionContext.opptegnelseRepository.alle().single().type)
        assertNull(sessionContext.venterPåKvitteringForOverstyringRepository.finn(meldingId))
    }

    @Language("JSON")
    private fun kvittering(meldingId: UUID, originaltEventName: String): String = """
        {
            "@event_name": "melding_om_melding_håndtert",
            "originalt_event_name" : "$originaltEventName",
            "original_id" : "$meldingId",
            "fødselsnummer" : "${lagIdentitetsnummer()}"
        }
    """.trimIndent()
}
