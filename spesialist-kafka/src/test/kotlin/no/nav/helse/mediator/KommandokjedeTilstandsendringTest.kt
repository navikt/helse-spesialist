package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.db.SessionContext
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class KommandokjedeTilstandsendringTest {
    private val mediator = UtgåendeMeldingerMediator()
    private val testRapid = TestRapid()
    private val publiserer = MessageContextMeldingPubliserer(testRapid)
    private val testmelding = object : Personmelding {
        override fun behandle(
            person: Person,
            kommandostarter: Kommandostarter,
            sessionContext: SessionContext,
            syncPersonTilDatabase: () -> Unit
        ) {}
        override fun fødselsnummer(): String = lagFødselsnummer()
        override fun toJson(): String = "{}"
        override val id: UUID = UUID.randomUUID()
    }

    @Test
    fun kommandokjede_ferdigstilt() {
        val navn = "En kommando"
        val contextId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val event = KommandokjedeEndretEvent.Ferdig(navn, contextId, hendelseId)
        mediator.tilstandEndret(event)
        mediator.publiserOppsamledeMeldinger(testmelding, publiserer)
        val melding = testRapid.inspektør.message(0)
        assertEquals("kommandokjede_ferdigstilt", melding["@event_name"].asText())
        assertEquals(contextId, melding["commandContextId"].asUUID())
        assertEquals(hendelseId, melding["meldingId"].asUUID())
        assertEquals(navn, melding["command"].asText())
    }

    @Test
    fun kommandokjede_suspendert() {
        val navn = "En kommando"
        val contextId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val event = KommandokjedeEndretEvent.Suspendert(navn, listOf(1, 2, 3), contextId, hendelseId)
        mediator.tilstandEndret(event)
        mediator.publiserOppsamledeMeldinger(testmelding, publiserer)
        val melding = testRapid.inspektør.message(0)
        assertEquals("kommandokjede_suspendert", melding["@event_name"].asText())
        assertEquals(contextId, melding["commandContextId"].asUUID())
        assertEquals(hendelseId, melding["meldingId"].asUUID())
        assertEquals(navn, melding["command"].asText())
        assertEquals(listOf(1, 2, 3), melding["sti"].map { it.asInt() })
    }

    @Test
    fun kommandokjede_avbrutt() {
        val contextId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val event = KommandokjedeEndretEvent.Avbrutt(contextId, hendelseId)
        mediator.tilstandEndret(event)
        mediator.publiserOppsamledeMeldinger(testmelding, publiserer)
        val melding = testRapid.inspektør.message(0)
        assertEquals("kommandokjede_avbrutt", melding["@event_name"].asText())
        assertEquals(contextId, melding["commandContextId"].asUUID())
        assertEquals(hendelseId, melding["meldingId"].asUUID())
    }
}
