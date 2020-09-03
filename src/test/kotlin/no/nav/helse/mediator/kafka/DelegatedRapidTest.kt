package no.nav.helse.mediator.kafka

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DelegatedRapidTest : River.PacketListener {
    private val testRapid = TestRapid()
    private val delegatedRapid = DelegatedRapid(testRapid, ::beforeRiver, ::afterRiver, ::errorHandler).apply {
        River(this)
            .register(this@DelegatedRapidTest)
    }

    private var error = false
    private val order = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        error = false
        order.clear()
    }

    @Test
    fun `callbacks are called in order`() {
        testRapid.sendTestMessage("{}")
        assertEquals(listOf("BEFORE", "PACKET", "AFTER"), order)
    }

    @Test
    fun `error handler is called on exceptions`() {
        testRapid.sendTestMessage("this_is_not_valid_json")
        assertTrue(error)
        assertEquals(listOf("BEFORE", "ERROR"), order)
    }

    private fun beforeRiver() {
        order.add("BEFORE")
    }
    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        order.add("PACKET")
    }

    private fun afterRiver(message: String, context: RapidsConnection.MessageContext) {
        order.add("AFTER")
    }

    private fun errorHandler(exception: Exception, message: String) {
        error = true
        order.add("ERROR")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        // rethrow to blow up the river
        throw error
    }
}
