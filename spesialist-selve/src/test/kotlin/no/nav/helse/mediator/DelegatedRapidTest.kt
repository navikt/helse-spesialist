package no.nav.helse.mediator

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DelegatedRapidTest : River.PacketListener {
    private val testRapid = TestRapid()
    init {
        DelegatedRapid(testRapid, ::beforeRiver, ::shouldProcess, ::afterRiver, ::errorHandler).apply {
            River(this).register(this@DelegatedRapidTest)
        }
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
        assertEquals(listOf("BEFORE", "SHOULD_PROCESS", "PACKET", "AFTER"), order)
    }

    @Test
    fun `error handler is called on exceptions`() {
        assertThrows<Exception> {
            testRapid.sendTestMessage("this_is_not_valid_json")
        }
        assertTrue(error)
        assertEquals(listOf("BEFORE", "SHOULD_PROCESS", "ERROR"), order)
    }

    private fun beforeRiver() {
        order.add("BEFORE")
    }
    private fun shouldProcess(message: String): Boolean{
        order.add("SHOULD_PROCESS")
        return true
    }
    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        order.add("PACKET")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun afterRiver(message: String) {
        order.add("AFTER")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun errorHandler(exception: Exception, message: String) {
        error = true
        order.add("ERROR")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        // rethrow to blow up the river
        throw error
    }
}
