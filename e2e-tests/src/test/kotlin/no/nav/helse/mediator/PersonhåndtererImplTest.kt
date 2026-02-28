package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.meldinger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonhåndtererImplTest : AbstractDatabaseTest() {
    private val testRapid = TestRapid()

    private val personhåndterer =
        PersonhåndtererImpl(
            publiserer = MessageContextMeldingPubliserer(testRapid),
        )

    @BeforeEach
    fun resetTestSetup() {
        testRapid.reset()
    }

    @Test
    fun `oppdater persondata`() {
        val fødselsnummer = lagFødselsnummer()
        personhåndterer.oppdaterPersondata(fødselsnummer)
        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertEquals("oppdater_persondata", sisteMelding["@event_name"].asText())
        assertEquals(fødselsnummer, sisteMelding["fødselsnummer"].asText())
    }

    @Test
    fun `klargjør person for visning`() {
        val fødselsnummer = lagFødselsnummer()
        personhåndterer.klargjørPersonForVisning(fødselsnummer)
        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertEquals("klargjør_person_for_visning", sisteMelding["@event_name"].asText())
        assertEquals(fødselsnummer, sisteMelding["fødselsnummer"].asText())
    }
}
