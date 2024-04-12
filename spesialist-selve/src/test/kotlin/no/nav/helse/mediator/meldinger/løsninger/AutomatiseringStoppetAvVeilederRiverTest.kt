package no.nav.helse.mediator.meldinger.løsninger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class AutomatiseringStoppetAvVeilederRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<MeldingMediator>(relaxed = true)

    init {
        AutomatiseringStoppetAvVeilederLøsning.AutomatiseringStoppetAvVeilederRiver(testRapid, mediator)
    }

    @ParameterizedTest
    @MethodSource("stoppårsakerArguments")
    fun `leser AutomatiseringStoppetAvVeileder`(årsaker: Set<String>) {
        testRapid.sendTestMessage(melding(true, årsaker))
        testRapid.sendTestMessage(melding(false, årsaker))
        verify(exactly = 2) {
            mediator.løsning(any(), any(), any(), any<AutomatiseringStoppetAvVeilederLøsning>(), any())
        }
    }

    companion object {
        @JvmStatic
        fun stoppårsakerArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(setOf<String>(), true),
            Arguments.of(setOf("stoppes nuh")),
            Arguments.of(setOf("stoppes nuh", "stopp denne"))
        )
    }

    @Language("json")
    fun melding(stoppet: Boolean, årsaker: Set<String>) = """
        {
            "@event_name": "behov",
            "@behov": [
                "AutomatiseringStoppetAvVeileder"
            ],
            "@id": "${UUID.randomUUID()}",
            "fødselsnummer": "11223312345",
            "@løsning": {
                "automatiseringStoppet": $stoppet,
                "årsaker": ${årsaker.map { "\"$it\"" }}
            },
            "@final": true,
            "hendelseId": "${UUID.randomUUID()}",
            "contextId": "${UUID.randomUUID()}"
        }
        """.trimIndent()
}
